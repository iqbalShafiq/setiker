package presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import presentation.common.rememberReduceMotion
import presentation.theme.AccentCoral
import presentation.theme.SetikerMotion

/**
 * Blocks pointer input while a screen is working, and says so with a slim brand line.
 *
 * It used to block silently: the user could tap and nothing happened. The line is deliberately
 * thin so it does not compete with the screen's own button spinner or progress dialog.
 */
@Composable
fun InteractionBlockedBox(
    blocked: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (blocked) {
            val reduceMotion = rememberReduceMotion()
            val transition = rememberInfiniteTransition(label = "blocked_bar")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = SetikerMotion.DURATION_SHIMMER_MS,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "blocked_bar_progress"
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .semantics { disabled() }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    drawRect(color = AccentCoral.copy(alpha = 0.18f))
                    if (reduceMotion) {
                        drawRect(color = AccentCoral.copy(alpha = 0.45f))
                    } else {
                        val band = size.width * 0.28f
                        val start = -band + progress * (size.width + band)
                        drawRect(
                            color = AccentCoral,
                            topLeft = Offset(start, 0f),
                            size = Size(band, size.height)
                        )
                    }
                }
            }
        }
    }
}
