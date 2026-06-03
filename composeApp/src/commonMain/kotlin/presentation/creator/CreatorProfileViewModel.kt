package presentation.creator

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

class CreatorProfileViewModel(
    private val exploreApiRepository: ExploreApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CreatorProfileState())
    val state: StateFlow<CreatorProfileState> = _state.asStateFlow()

    private val _effect = Channel<CreatorProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var userId: String = ""

    fun onIntent(intent: CreatorProfileIntent) {
        when (intent) {
            is CreatorProfileIntent.Load -> load(intent.userId)
            is CreatorProfileIntent.ChangeSort -> {
                _state.update { it.copy(sort = intent.sort) }
                reloadPacks()
            }
            CreatorProfileIntent.ToggleFollow -> toggleFollow()
            is CreatorProfileIntent.OpenPack -> {
                viewModelScope.launch { _effect.send(CreatorProfileEffect.NavigateToPack(intent.packId)) }
            }
            CreatorProfileIntent.NavigateBack -> {
                viewModelScope.launch { _effect.send(CreatorProfileEffect.NavigateBack) }
            }
        }
    }

    private fun load(id: String) {
        userId = id
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false) }
            runCatching {
                val profile = exploreApiRepository.getPublicUserProfile(id)
                val packs = exploreApiRepository.getPublicUserPacks(id, page = 1, limit = 20, sort = _state.value.sort)
                profile to packs.data
            }.onSuccess { (profile, packs) ->
                _state.update { it.copy(isLoading = false, profile = profile, packs = packs) }
            }.onFailure {
                _state.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private fun reloadPacks() {
        if (userId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                exploreApiRepository.getPublicUserPacks(userId, page = 1, limit = 20, sort = _state.value.sort).data
            }.onSuccess { packs ->
                _state.update { it.copy(packs = packs) }
            }
        }
    }

    private fun toggleFollow() {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            _state.update { it.copy(isFollowLoading = true) }
            runCatching {
                if (profile.isFollowing) exploreApiRepository.unfollowUser(profile.id)
                else exploreApiRepository.followUser(profile.id)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isFollowLoading = false,
                        profile = profile.copy(
                            isFollowing = result.following,
                            followerCount = result.followerCount
                        )
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isFollowLoading = false) }
                _effect.send(CreatorProfileEffect.ShowMessage(UiText.DynamicString(error.message ?: "Follow failed")))
            }
        }
    }
}
