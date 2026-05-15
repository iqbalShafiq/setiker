package presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalDialogRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/**
 * Reusable Neubrutal progress dialog. Used by long-running work like:
 * - VideoCrop "Apply" (decoding frames)
 * - AnimatedEditor "Save" (composing decorations + WebP encoding)
 *
 * Showing this dialog blocks user interaction. Pass a real `progress` value
 * in `0f..1f`; the implementations are responsible for forwarding actual progress
 * from the storage layer instead of dummy values.
 */
@Composable
fun ProgressDialog(
    title: String,
    progress: Float,
    progressLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { /* not dismissable while work is running */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        val animated by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 180),
            label = "progress_dialog_value"
        )
        val border = neubrutalBorderColor()
        val shadow = neubrutalShadowColor()
        val shape = RoundedCornerShape(NeubrutalDialogRadius)
        val barShape = RoundedCornerShape(NeubrutalShadowOffset)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .neubrutalShadow(
                    offsetX = NeubrutalShadowOffset,
                    offsetY = NeubrutalShadowOffset,
                    cornerRadius = NeubrutalDialogRadius,
                    color = shadow
                )
                .clip(shape)
                .background(neubrutalCardSurface())
                .border(NeubrutalBorderWidth, border, shape)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar with neubrutal frame around it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .neubrutalShadow(
                        offsetX = NeubrutalShadowOffset,
                        offsetY = NeubrutalShadowOffset,
                        cornerRadius = NeubrutalShadowOffset,
                        color = shadow
                    )
                    .clip(barShape)
                    .background(neubrutalCardSurface())
                    .border(NeubrutalBorderWidth, border, barShape)
                    .padding(2.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(barShape),
                    color = AccentCoral,
                    trackColor = neubrutalCardSurface()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progressLabel.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                Text(
                    text = "${(animated * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = neubrutalOnSurface()
                )
            }
        }
    }
}

// MARK: - Previews
@Preview
@Composable
private fun ProgressDialogPreview() {
    MaterialTheme {
        ProgressDialog(
            title = "Exporting Video...",
            progress = 0.65f,
            progressLabel = "Frame 12/24"
        )
    }
}
