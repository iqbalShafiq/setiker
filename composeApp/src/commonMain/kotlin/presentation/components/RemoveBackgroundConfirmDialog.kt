package presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.remove_bg_confirm_action
import setiker.composeapp.generated.resources.remove_bg_confirm_message
import setiker.composeapp.generated.resources.remove_bg_confirm_title

@Composable
fun RemoveBackgroundConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        title = stringResource(Res.string.remove_bg_confirm_title),
        message = stringResource(Res.string.remove_bg_confirm_message),
        confirmText = stringResource(Res.string.remove_bg_confirm_action),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDanger = false
    )
}
