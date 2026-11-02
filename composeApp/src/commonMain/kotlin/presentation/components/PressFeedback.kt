package presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.drawNeubrutalShadow

/**
 * Press feedback shared by every tappable claymorphism control: the interaction source to hand to
 * `clickable`, the raw [isPressed] flag (used to shorten the drop shadow) and the animated [scale].
 *
 * [scale] and [shadowProgress] stay [State]s so callers can apply them with [Modifier.pressScale]
 * and [Modifier.pressShadow], which read them in the draw phase: the press animation invalidates
 * that layer instead of recomposing the screen.
 */
@Stable
class PressFeedback(
    val interactionSource: MutableInteractionSource,
    val isPressed: Boolean,
    val scale: State<Float>,
    val shadowProgress: State<Float>
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
        ),
        shadowProgress = animateFloatAsState(
            targetValue = if (isPressed && enabled) 0f else 1f,
            animationSpec = SetikerMotion.PressSpring,
            label = "${label}_shadow"
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
 * Draws the hard neubrutal shadow with an offset that follows the press.
 *
 * The value is read in the draw phase, so the shadow shrinks together with the scale without a
 * recomposition. The rest offset defaults to the card shadow; controls with a smaller surface can
 * pass [NeubrutalSmallShadowOffset] as both the rest and pressed offset.
 */
fun Modifier.pressShadow(
    feedback: PressFeedback,
    restOffset: Dp = NeubrutalShadowOffset,
    pressedOffset: Dp = NeubrutalSmallShadowOffset,
    cornerRadius: Dp = NeubrutalCardRadius,
    color: Color = NeubrutalBlack
): Modifier = drawBehind {
    val progress = feedback.shadowProgress.value
    val offset = (pressedOffset + (restOffset - pressedOffset) * progress).toPx()
    drawNeubrutalShadow(
        offsetX = offset,
        offsetY = offset,
        cornerRadius = cornerRadius.toPx(),
        color = color
    )
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
    role: Role? = null,
    onClick: () -> Unit
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    role = role,
    onClick = onClick
)

/** [combinedClickable] with the same no-ripple press language as [clickableNoRipple]. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableNoRipple(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = combinedClickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    role = role,
    onClick = onClick,
    onLongClick = onLongClick
)
