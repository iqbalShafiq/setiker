package presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.improve_confirm_action
import setiker.composeapp.generated.resources.improve_confirm_title

@Composable
fun ImproveConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        title = stringResource(Res.string.improve_confirm_title),
        message = message,
        confirmText = stringResource(Res.string.improve_confirm_action),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDanger = false
    )
}
