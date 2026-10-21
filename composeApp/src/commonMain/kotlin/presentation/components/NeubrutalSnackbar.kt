package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.components.AppSnackbarHostState
import presentation.components.NeubrutalSnackbarHost
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalPressable
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/** How long a message stays before it dismisses itself, unless the user closes it first. */
internal const val DEFAULT_AUTO_DISMISS_MS = 10_000L

/**
 * How long a failure stays.
 *
 * Longer than a message, because a failure is the one thing in the app that the user has to act
 * on. Ten seconds is Material's "long" snackbar; on a phone that takes a while to answer, half
 * of that is gone before the request has even finished.
 */
internal const val ERROR_AUTO_DISMISS_MS = 15_000L

/**
 * The app's snackbar.
 *
 * The stock Material snackbar is a rounded grey pill, which does not belong here: every other
 * surface in this app has a thick border and a hard offset shadow. This one keeps Material's
 * machinery - the state machine, the placement above the bottom bar, the enter and exit
 * animation - and replaces the look.
 *
 * The error variant is the one that matters most. An error that fails to look like an error gets
 * read as a note, and a note gets ignored. So a failure carries the error colour, a solid icon in
 * a filled circle, and a bold message, and it says so to a screen reader as well.
 *
 * The snackbar dismisses itself after [autoDismissMs], and the close button ends it right away.
 */
@Composable
fun NeubrutalSnackbarHost(
    state: AppSnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = state.hostState,
        modifier = modifier,
        snackbar = { data ->
            // The kind of message travels with the snackbar, so the treatment is never a guess
            // about the state of the screen.
            NeubrutalSnackbar(
                data = data,
                isError = state.isError,
                autoDismissMs = if (state.isError) ERROR_AUTO_DISMISS_MS else DEFAULT_AUTO_DISMISS_MS
            )
        }
    )
}

/**
 * The snackbar itself.
 *
 * [isError] switches on the danger treatment: the error container as the background, a full
 * strength error border, a solid icon and a bold message.
 */
@Composable
fun NeubrutalSnackbar(
    data: SnackbarData,
    isError: Boolean = false,
    autoDismissMs: Long = DEFAULT_AUTO_DISMISS_MS,
    modifier: Modifier = Modifier
) {
    NeubrutalSnackbarContent(
        message = data.visuals.message,
        actionLabel = data.visuals.actionLabel,
        isError = isError,
        autoDismissMs = autoDismissMs,
        onAction = { data.performAction(); data.dismiss() },
        onDismiss = { data.dismiss() },
        modifier = modifier
    )
}

/**
 * The look of the snackbar, without the state machinery.
 *
 * Splitting it out is what makes it testable: the screenshot test draws this one, and the
 * behaviour test drives the wrapper.
 */
@Composable
fun NeubrutalSnackbarContent(
    message: String,
    isError: Boolean = false,
    actionLabel: String? = null,
    autoDismissMs: Long = DEFAULT_AUTO_DISMISS_MS,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val surface = if (isError) MaterialTheme.colorScheme.errorContainer else neubrutalCardSurface()
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    val border = if (isError) MaterialTheme.colorScheme.error else neubrutalBorderColor()

    // The timer is one animation: it drains the line and, when it is done, the snackbar goes
    // away. Reading the value in the draw phase keeps the frame work out of composition.
    val timer = remember { Animatable(1f) }
    LaunchedEffect(message) {
        timer.snapTo(1f)
        timer.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = autoDismissMs.toInt(), easing = LinearEasing)
        )
        onDismiss()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = neubrutalShadowColor()
            )
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(surface)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Box {
        Row(verticalAlignment = Alignment.Top) {
            if (isError) {
                // The same shape as every other icon button in the app: a square with a thick
                // border and a hard shadow, filled with the error colour.
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .neubrutalShadow(
                            offsetX = NeubrutalSmallShadowOffset,
                            offsetY = NeubrutalSmallShadowOffset,
                            cornerRadius = NeubrutalSmallRadius,
                            color = neubrutalShadowColor()
                        )
                        .clip(RoundedCornerShape(NeubrutalSmallRadius))
                        .background(MaterialTheme.colorScheme.error)
                        .neubrutalBorderWithGloss(
                            color = neubrutalBorderColor(),
                            cornerRadius = NeubrutalSmallRadius,
                            highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    // The close button floats over the top right corner, so the message keeps
                    // clear of it.
                    .padding(end = 40.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isError) FontWeight.Bold else FontWeight.Medium,
                    color = content,
                    // A snackbar that appears without being announced is a snackbar a screen
                    // reader user never learns about. Polite is the right level: it waits for a
                    // pause in the speech instead of interrupting it.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )

                if (actionLabel != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.neubrutalPressable(
                            pressScale = 0.94f,
                            onClick = onAction
                        )
                    )
                }
            }

        }

        // The close button floats over the top right corner. Its 48 dp touch target stays out
        // of the flow, so the card is only as tall as the message needs, and the glyph sits at
        // the top of it, on the first line of the message.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .neubrutalPressable(pressScale = 0.88f, onClick = onDismiss),
            contentAlignment = Alignment.TopCenter
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp)
            )
        }
        }
    }
}
