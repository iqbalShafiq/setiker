package presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.unfollow_confirm_action
import setiker.composeapp.generated.resources.unfollow_confirm_message
import setiker.composeapp.generated.resources.unfollow_confirm_title

@Composable
fun UnfollowConfirmDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true
) {
    AppDialog(
        title = stringResource(Res.string.unfollow_confirm_title),
        message = stringResource(Res.string.unfollow_confirm_message, displayName),
        confirmText = stringResource(Res.string.unfollow_confirm_action),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDanger = false,
        confirmEnabled = confirmEnabled
    )
}
