package presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.ExploreApiRepository
import data.remote.ExploreSort
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText

class ExploreViewModel(
    private val exploreApiRepository: ExploreApiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    private val _effect = Channel<ExploreEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: ExploreIntent) {
        when (intent) {
            ExploreIntent.LoadInitial -> loadInitial()
            ExploreIntent.LoadMore -> loadMore()
            is ExploreIntent.ChangeSort -> changeSort(intent.sort)
            is ExploreIntent.SearchChanged -> _state.update { it.copy(searchQuery = intent.query) }
            is ExploreIntent.OpenPack -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateToPublicPack(intent.packId)) }
            }
            ExploreIntent.NavigateBack -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateBack) }
            }
            ExploreIntent.NavigateHistory -> {
                viewModelScope.launch { _effect.send(ExploreEffect.NavigateHistory) }
            }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            val current = _state.value
            _state.update { it.copy(isLoading = true, error = null, page = 1) }
            runCatching {
                exploreApiRepository.getPublicPacks(page = 1, limit = current.limit, sort = current.sort)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        packs = result.data,
                        page = result.page,
                        totalPages = result.totalPages
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load packs") }
                _effect.send(ExploreEffect.ShowError(UiText.DynamicString(error.message ?: "Failed to load packs")))
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
                exploreApiRepository.getPublicPacks(page = nextPage, limit = current.limit, sort = current.sort)
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
                _effect.send(ExploreEffect.ShowError(UiText.DynamicString(error.message ?: "Failed to load more packs")))
            }
        }
    }

    private fun changeSort(sort: ExploreSort) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort, packs = emptyList(), page = 1, totalPages = 1) }
        loadInitial()
    }
}
