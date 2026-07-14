package presentation.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.ExploreApiRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.blocked_users_load_failed
import setiker.composeapp.generated.resources.blocked_users_unblock_success

class BlockedUsersViewModel(
    private val exploreApiRepository: ExploreApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BlockedUsersState())
    val state: StateFlow<BlockedUsersState> = _state.asStateFlow()

    private val _effect = Channel<BlockedUsersEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(BlockedUsersIntent.Load)
    }

    fun onIntent(intent: BlockedUsersIntent) {
        when (intent) {
            BlockedUsersIntent.Load -> load()
            BlockedUsersIntent.Retry -> load()
            BlockedUsersIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(BlockedUsersEffect.NavigateBack)
            }
            is BlockedUsersIntent.ShowUnblockConfirm -> _state.update {
                it.copy(pendingUnblockUserId = intent.userId)
            }
            BlockedUsersIntent.DismissUnblockConfirm -> _state.update {
                it.copy(pendingUnblockUserId = null)
            }
            BlockedUsersIntent.ConfirmUnblock -> unblock()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false) }
            runCatching { exploreApiRepository.listBlockedUsers() }
                .onSuccess { data ->
                    _state.update { it.copy(isLoading = false, loadFailed = false, users = data.users) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun unblock() {
        val userId = _state.value.pendingUnblockUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(unblockingUserId = userId, pendingUnblockUserId = null) }
            runCatching { exploreApiRepository.unblockUser(userId) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            unblockingUserId = null,
                            users = it.users.filterNot { user -> user.id == userId }
                        )
                    }
                    _effect.send(BlockedUsersEffect.ShowMessage(UiText.StringRes(Res.string.blocked_users_unblock_success)))
                }
                .onFailure { error ->
                    _state.update { it.copy(unblockingUserId = null) }
                    _effect.send(BlockedUsersEffect.ShowMessage(error.toUiText(Res.string.blocked_users_load_failed)))
                }
        }
    }
}
