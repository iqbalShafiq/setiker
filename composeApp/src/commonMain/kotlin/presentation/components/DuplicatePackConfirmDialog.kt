package presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.pack_duplicate
import setiker.composeapp.generated.resources.pack_duplicate_dialog_message
import setiker.composeapp.generated.resources.pack_duplicate_dialog_title

@Composable
fun DuplicatePackConfirmDialog(
    packName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true
) {
    AppDialog(
        title = stringResource(Res.string.pack_duplicate_dialog_title),
        message = stringResource(Res.string.pack_duplicate_dialog_message, packName),
        confirmText = stringResource(Res.string.pack_duplicate),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDanger = false,
        confirmEnabled = confirmEnabled
    )
}
