package presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import presentation.theme.SetikerMotion

/**
 * Press feedback shared by every tappable claymorphism control: the interaction source to hand to
 * `clickable`, the raw [isPressed] flag (used to shorten the drop shadow) and the animated [scale].
 *
 * [scale] stays a [State] so callers can apply it with [Modifier.pressScale], which reads it in the
 * draw phase: the press animation invalidates that layer instead of recomposing the screen.
 */
@Stable
class PressFeedback(
    val interactionSource: MutableInteractionSource,
    val isPressed: Boolean,
    val scale: State<Float>
)

/**
 * Remembers the press interaction of a control. [pressedScale] is the scale while pressed; while
 * [enabled] is false the control stays at 1f.
 *
 * The spring is deliberately **not** bouncy: these controls are pressed dozens of times a day and
 * an overshoot on release reads as sloppiness. Use [SetikerMotion.DelightSpring] explicitly for the
 * rare, celebratory moments.
 */
@Composable
fun rememberPressFeedback(
    pressedScale: Float,
    enabled: Boolean = true,
    label: String = "press_scale"
): PressFeedback {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    return PressFeedback(
        interactionSource = interactionSource,
        isPressed = isPressed,
        scale = animateFloatAsState(
            targetValue = if (isPressed && enabled) pressedScale else 1f,
            animationSpec = SetikerMotion.PressSpring,
            label = label
        )
    )
}

/** Applies [PressFeedback.scale] without recomposing while the press animation runs. */
fun Modifier.pressScale(feedback: PressFeedback): Modifier =
    graphicsLayer {
        scaleX = feedback.scale.value
        scaleY = feedback.scale.value
    }

/**
 * A clickable without the Material ripple.
 *
 * Every control in this app shows its press state with a scale + shadow change. Leaving the
 * Material ripple on a control made two different press languages live in the same product.
 */
fun Modifier.clickableNoRipple(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = onClick
)
