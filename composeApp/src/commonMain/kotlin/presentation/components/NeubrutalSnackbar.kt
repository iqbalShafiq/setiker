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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalPressable
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/** How long a snackbar stays before it dismisses itself, unless the user closes it first. */
private const val DEFAULT_AUTO_DISMISS_MS = 10_000L

/** The height of the draining timer line along the bottom of the snackbar. */
private val TimerBarHeight = 4.dp

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
 * The snackbar dismisses itself after [autoDismissMs] and shows a draining line while it waits,
 * so the user can see how long they have. The close button ends it right away.
 */
@Composable
fun NeubrutalSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    autoDismissMs: Long = DEFAULT_AUTO_DISMISS_MS
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            NeubrutalSnackbar(
                data = data,
                isError = isError,
                autoDismissMs = autoDismissMs
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
    val timerColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

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
        Row(verticalAlignment = Alignment.Top) {
            if (isError) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
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

            Column(modifier = Modifier.weight(1f)) {
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

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .neubrutalPressable(pressScale = 0.88f, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // The draining line: the time the user has left, made visible.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TimerBarHeight)
                .clip(RoundedCornerShape(TimerBarHeight))
                .background(timerColor.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TimerBarHeight)
                    .drawTimerFill(timerColor, timer)
            )
        }
    }
}

/**
 * Fills the timer line by the fraction that is left.
 *
 * The animated value is read in the draw phase, so the line drains without a recomposition on
 * every frame.
 */
private fun Modifier.drawTimerFill(color: Color, timer: Animatable<Float, *>) = drawBehind {
        val fraction = timer.value.coerceIn(0f, 1f)
        if (fraction <= 0f) return@drawBehind
        drawRect(
            color = color,
            size = Size(width = size.width * fraction, height = size.height)
        )
    }
