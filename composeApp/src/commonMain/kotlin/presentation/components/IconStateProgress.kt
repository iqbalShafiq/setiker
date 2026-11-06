package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import presentation.common.rememberReduceMotion
import presentation.theme.SetikerMotion

/**
 * The one clock every stateful glyph reads.
 *
 * Filling and draining are asymmetric, the first composition never animates, and reduced motion
 * snaps instead of sliding. Those three rules were about to be written a third time, so they live
 * here; [StatefulIcon] and [MorphGlyphIcon] differ in how they *draw* a state change, not in how
 * they time one.
 *
 * @return 0 for the resting state, 1 for the activated state, as a state that can be read in a
 *   draw phase without recomposing anything.
 */
@Composable
internal fun rememberStateProgress(
    checked: Boolean,
    fillMs: Int = SetikerMotion.ICON_FILL_MS,
    drainMs: Int = SetikerMotion.ICON_DRAIN_MS,
    /**
     * Animate on the first composition instead of starting at the end state.
     *
     * Only for a glyph that is *mounted because the state changed* — a tick that appears when a
     * save lands. Everywhere else a screen that opens on a checked state must not replay it.
     */
    playOnEnter: Boolean = false
): State<Float> {
    val reduceMotion = rememberReduceMotion()
    val progress = remember {
        Animatable(if (!playOnEnter && checked) 1f else 0f)
    }
    var armed by remember { mutableStateOf(playOnEnter) }

    LaunchedEffect(checked, reduceMotion, fillMs, drainMs) {
        if (!armed) {
            // A screen that opens on an already-checked state shows the end state, it does not
            // replay it in front of the user.
            armed = true
            return@LaunchedEffect
        }
        val target = if (checked) 1f else 0f
        if (reduceMotion) {
            progress.snapTo(target)
            return@LaunchedEffect
        }
        progress.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (checked) fillMs else drainMs,
                easing = SetikerMotion.EaseOut
            )
        )
    }

    return remember(progress) { progress.asState() }
}
