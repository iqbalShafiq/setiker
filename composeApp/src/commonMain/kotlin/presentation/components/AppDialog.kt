package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalDialogRadius
import presentation.theme.NeubrutalLargeShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.delete
import setiker.composeapp.generated.resources.delete_pack_dialog_message
import setiker.composeapp.generated.resources.delete_pack_dialog_title

@Composable
fun AppDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    isDanger: Boolean = true,
    confirmEnabled: Boolean = true
) {
    val resolvedDismissText = dismissText ?: stringResource(Res.string.cancel)
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalDialogRadius)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .neubrutalShadow(
                    offsetX = NeubrutalLargeShadowOffset,
                    offsetY = NeubrutalLargeShadowOffset,
                    cornerRadius = NeubrutalDialogRadius,
                    color = shadow
                )
                .clip(shape)
                .background(neubrutalCardSurface())
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = NeubrutalDialogRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .padding(28.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalMutedOnSurface(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(28.dp))

            AppPrimaryButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = confirmEnabled
            )

            Spacer(modifier = Modifier.height(10.dp))

            AppSecondaryButton(
                text = resolvedDismissText,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun AppDialogPreview() {
    MaterialTheme {
        AppDialog(
            title = stringResource(Res.string.delete_pack_dialog_title),
            message = stringResource(Res.string.delete_pack_dialog_message, "Sample Pack"),
            confirmText = stringResource(Res.string.delete),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
