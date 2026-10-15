package presentation.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.ExploreApiRepository
import data.remote.ExploreSort
import data.remote.model.applyFollowUpdate
import data.remote.model.withOptimisticFollow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.block_creator_success

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
            CreatorProfileIntent.ShowBlockCreatorConfirm -> {
                _state.update { it.copy(showBlockCreatorConfirm = true) }
            }
            CreatorProfileIntent.DismissBlockCreatorConfirm -> {
                _state.update { it.copy(showBlockCreatorConfirm = false) }
            }
            CreatorProfileIntent.ConfirmBlockCreator -> blockCreator()
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
            val snapshot = _state.value
            val wasFollowing = profile.isFollowing
            val targetFollowing = !wasFollowing
            applyOptimisticFollow(targetFollowing)
            runCatching {
                if (wasFollowing) exploreApiRepository.unfollowUser(profile.id)
                else exploreApiRepository.followUser(profile.id)
            }.onSuccess { result ->
                _state.update { current ->
                    current.copy(profile = current.profile?.applyFollowUpdate(result))
                }
            }.onFailure { error ->
                restoreFollowSnapshot(snapshot)
                _effect.send(CreatorProfileEffect.ShowMessage(UiText.DynamicString(error.message ?: "Follow failed")))
            }
        }
    }

    private fun applyOptimisticFollow(following: Boolean) {
        _state.update { current ->
            current.copy(profile = current.profile?.withOptimisticFollow(following))
        }
    }

    private fun restoreFollowSnapshot(snapshot: CreatorProfileState) {
        _state.update { it.copy(profile = snapshot.profile) }
    }

    private fun blockCreator() {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            _state.update { it.copy(isBlockingCreator = true) }
            runCatching {
                exploreApiRepository.blockUser(profile.id)
            }.onSuccess {
                _state.update {
                    it.copy(
                        isBlockingCreator = false,
                        showBlockCreatorConfirm = false
                    )
                }
                _effect.send(
                    CreatorProfileEffect.ShowMessage(
                        UiText.StringRes(Res.string.block_creator_success)
                    )
                )
                _effect.send(CreatorProfileEffect.NavigateBack)
            }.onFailure { error ->
                _state.update { it.copy(isBlockingCreator = false, showBlockCreatorConfirm = false) }
                _effect.send(
                    CreatorProfileEffect.ShowMessage(
                        UiText.DynamicString(error.message ?: "Block failed")
                    )
                )
            }
        }
    }
}
