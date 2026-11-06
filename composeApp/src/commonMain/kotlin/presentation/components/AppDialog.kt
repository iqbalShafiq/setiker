package presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import presentation.theme.NeubrutalDialogRadius
import presentation.theme.NeubrutalLargeShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.delete
import setiker.composeapp.generated.resources.delete_pack_dialog_message
import setiker.composeapp.generated.resources.delete_pack_dialog_title

/** Material-style dialog width: near full-bleed on phones, capped on large screens. */
private val AppDialogMaxWidth = 560.dp
private val AppDialogHorizontalMargin = 6.dp

@Composable
fun AppDialogWidthContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val dialogWidth = minOf(
            maxWidth - AppDialogHorizontalMargin * 2,
            AppDialogMaxWidth
        )
        Box(
            modifier = Modifier.width(dialogWidth),
            content = content
        )
    }
}

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
    confirmEnabled: Boolean = true,
    showDismissButton: Boolean = true
) {
    val resolvedDismissText = dismissText ?: stringResource(Res.string.cancel)
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalDialogRadius)

    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    Dialog(onDismissRequest = onDismiss) {
        AppDialogWidthContainer {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = SetikerMotion.DURATION_FAST_MS,
                        easing = SetikerMotion.EaseOut
                    )
                ) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(
                        durationMillis = SetikerMotion.DURATION_FAST_MS,
                        easing = SetikerMotion.EaseOut
                    )
                )
            ) {
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
                        .neubrutalFill(color = neubrutalCardSurface(), cornerRadius = NeubrutalDialogRadius)
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

                    if (showDismissButton) {
                        Spacer(modifier = Modifier.height(10.dp))

                        AppSecondaryButton(
                            text = resolvedDismissText,
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
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
