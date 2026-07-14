package presentation.blocked

import data.remote.model.BlockedUserItem
import presentation.common.UiText

data class BlockedUsersState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val users: List<BlockedUserItem> = emptyList(),
    val pendingUnblockUserId: String? = null,
    val unblockingUserId: String? = null
)

sealed interface BlockedUsersIntent {
    data object Load : BlockedUsersIntent
    data object Retry : BlockedUsersIntent
    data object NavigateBack : BlockedUsersIntent
    data class ShowUnblockConfirm(val userId: String) : BlockedUsersIntent
    data object DismissUnblockConfirm : BlockedUsersIntent
    data object ConfirmUnblock : BlockedUsersIntent
}

sealed interface BlockedUsersEffect {
    data object NavigateBack : BlockedUsersEffect
    data class ShowMessage(val message: UiText) : BlockedUsersEffect
}
