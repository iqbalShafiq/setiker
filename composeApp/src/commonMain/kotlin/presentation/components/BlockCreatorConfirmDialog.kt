package presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.block_creator_confirm
import setiker.composeapp.generated.resources.block_creator_message
import setiker.composeapp.generated.resources.block_creator_title
import setiker.composeapp.generated.resources.cancel

@Composable
fun BlockCreatorConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    AppDialog(
        title = stringResource(Res.string.block_creator_title),
        message = stringResource(Res.string.block_creator_message),
        confirmText = stringResource(Res.string.block_creator_confirm),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = !isLoading,
        isDanger = true
    )
}
