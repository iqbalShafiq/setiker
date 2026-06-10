package presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.remote.ExploreApiRepository
import data.remote.ExploreFeed
import data.remote.ExploreSort
import data.remote.model.CloudStickerPack
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_load_explore_failed

class ExploreViewModel(
    private val exploreApiRepository: ExploreApiRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    private val _effect = Channel<ExploreEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var searchJob: Job? = null

    fun onIntent(intent: ExploreIntent) {
        when (intent) {
            ExploreIntent.LoadInitial -> {
                loadFeatured()
                loadInitial(refreshing = false)
            }
            ExploreIntent.Refresh -> loadInitial(refreshing = true)
            ExploreIntent.LoadMore -> loadMore()
            is ExploreIntent.ChangeSort -> changeSort(intent.sort)
            is ExploreIntent.ChangeFeed -> changeFeed(intent.feed)
            is ExploreIntent.SearchChanged -> onSearchChanged(intent.query)
            is ExploreIntent.OpenPack -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateToPublicPack(intent.packId)) }
            }
            is ExploreIntent.OpenCreator -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateToCreator(intent.userId)) }
            }
            is ExploreIntent.ToggleLike -> toggleLike(intent.packId)
            is ExploreIntent.ToggleSave -> toggleSave(intent.packId)
            ExploreIntent.NavigateBack -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateBack) }
            }
            ExploreIntent.NavigateHistory -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateHistory) }
            }
            ExploreIntent.NavigateLogin -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateLogin) }
            }
        }
    }

    private fun requiresAuth(feed: ExploreFeed): Boolean =
        feed != ExploreFeed.DISCOVER

    private fun onSearchChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadInitial(refreshing = false)
        }
    }

    private fun changeFeed(feed: ExploreFeed) {
        if (_state.value.feed == feed) return
        viewModelScope.launch {
            if (requiresAuth(feed) && !authManager.isAuthenticated()) {
                _state.update {
                    it.copy(
                        feed = feed,
                        requiresLogin = true,
                        packs = emptyList(),
                        isLoading = false,
                        loadFailed = false
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    feed = feed,
                    requiresLogin = false,
                    packs = emptyList(),
                    page = 1,
                    totalPages = 1
                )
            }
            if (feed == ExploreFeed.DISCOVER) {
                loadFeatured()
            } else {
                _state.update { it.copy(featuredPack = null) }
            }
            loadInitial()
        }
    }

    private fun loadFeatured() {
        viewModelScope.launch {
            runCatching { exploreApiRepository.getFeaturedToday() }
                .onSuccess { featured ->
                    _state.update { it.copy(featuredPack = featured?.pack) }
                }
        }
    }

    private fun loadInitial(refreshing: Boolean = false) {
        viewModelScope.launch {
            val current = _state.value
            if (requiresAuth(current.feed) && !authManager.isAuthenticated()) {
                _state.update {
                    it.copy(
                        requiresLogin = true,
                        isLoading = false,
                        isRefreshing = false,
                        packs = emptyList()
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = !refreshing,
                    isRefreshing = refreshing,
                    loadFailed = false,
                    error = null,
                    page = 1,
                    requiresLogin = false,
                    isAuthenticated = authManager.isAuthenticated()
                )
            }
            runCatching {
                exploreApiRepository.getPublicPacks(
                    page = 1,
                    limit = current.limit,
                    sort = current.sort,
                    q = current.searchQuery.takeIf { it.isNotBlank() },
                    feed = current.feed
                )
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        loadFailed = false,
                        packs = result.data,
                        page = result.page,
                        totalPages = result.totalPages
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, isRefreshing = false, loadFailed = true)
                }
                _effect.send(ExploreEffect.ShowError(error.toUiText(Res.string.error_load_explore_failed)))
            }
        }
    }

    private fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore || current.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val nextPage = current.page + 1
            runCatching {
                exploreApiRepository.getPublicPacks(
                    page = nextPage,
                    limit = current.limit,
                    sort = current.sort,
                    q = current.searchQuery.takeIf { it.isNotBlank() },
                    feed = current.feed
                )
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        packs = (it.packs + result.data).distinctBy { pack -> pack.id },
                        page = result.page,
                        totalPages = result.totalPages
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoadingMore = false) }
                _effect.send(ExploreEffect.ShowError(UiText.DynamicString(error.message ?: "Failed to load more")))
            }
        }
    }

    private fun changeSort(sort: ExploreSort) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort, packs = emptyList(), page = 1, totalPages = 1) }
        loadInitial()
    }

    private fun toggleLike(packId: String) {
        viewModelScope.launch {
            if (!authManager.isAuthenticated()) {
                _effect.send(ExploreEffect.NavigateLogin)
                return@launch
            }
            val pack = findPack(packId) ?: return@launch
            val liked = pack.isLiked ?: pack.liked ?: false
            runCatching {
                if (liked) exploreApiRepository.unlikePack(packId)
                else exploreApiRepository.likePack(packId)
            }.onSuccess { social ->
                _state.update { current -> current.withPackSocial(packId, social) }
            }.onFailure { error ->
                _effect.send(ExploreEffect.ShowError(UiText.DynamicString(error.message ?: "Like failed")))
            }
        }
    }

    private fun toggleSave(packId: String) {
        viewModelScope.launch {
            if (!authManager.isAuthenticated()) {
                _effect.send(ExploreEffect.NavigateLogin)
                return@launch
            }
            val pack = findPack(packId) ?: return@launch
            val saved = pack.isSaved ?: pack.saved ?: false
            runCatching {
                if (saved) exploreApiRepository.unsavePack(packId)
                else exploreApiRepository.savePack(packId)
            }.onSuccess { social ->
                _state.update { current -> current.withPackSocial(packId, social) }
            }.onFailure { error ->
                _effect.send(ExploreEffect.ShowError(UiText.DynamicString(error.message ?: "Save failed")))
            }
        }
    }

    private fun findPack(packId: String): CloudStickerPack? {
        val current = _state.value
        return current.packs.find { it.id == packId }
            ?: current.featuredPack?.takeIf { it.id == packId }
    }

    private fun ExploreState.withPackSocial(
        packId: String,
        social: data.remote.model.PackSocialStateData
    ): ExploreState {
        val updated = { pack: CloudStickerPack ->
            if (pack.id == packId) pack.withSocial(social) else pack
        }
        return copy(
            packs = packs.map(updated),
            featuredPack = featuredPack?.let(updated)
        )
    }

    private fun CloudStickerPack.withSocial(social: data.remote.model.PackSocialStateData): CloudStickerPack =
        copy(
            likeCount = social.likeCount ?: likeCount,
            saveCount = social.saveCount ?: saveCount,
            downloadCount = social.downloadCount ?: downloadCount,
            liked = social.liked,
            saved = social.saved,
            isLiked = social.liked,
            isSaved = social.saved
        )
}
