package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import presentation.common.rememberReduceMotion
import presentation.theme.SetikerMotion

/**
 * The glyph's acknowledgement of a landed state change.
 *
 * The app had three copies of this idea — Explore's like, Explore's save and the follow FAB each
 * hand-rolled an `Animatable(1f)` with a `hasAppeared` guard, a `snapTo` and a `DelightSpring`
 * decay. They disagreed about the overshoot (1.35 / 1.28 / 1.24) and none of them respected reduced
 * motion. This is the one implementation.
 *
 * Motion is the *result* of an action, never the press: a bar icon is pressed tens of times a day,
 * and an icon that twitches on every tap reads as latency rather than delight. The press belongs to
 * the container (`rememberPressFeedback`), the outcome belongs here.
 */
@Stable
class IconBeat(
    val scale: State<Float>,
    val rotationZ: State<Float>
)

/**
 * Remembers the beat for [trigger]. The beat plays when [trigger] changes, and only when it
 * changes: a screen that opens already showing a liked heart should not pop it.
 *
 * @param playOnEnter play once on the first composition instead of waiting for a change. Only for
 *   a glyph that appears *because* of a state, not for a state that was already true on arrival.
 */
@Composable
fun rememberIconBeat(
    trigger: Any?,
    playOnEnter: Boolean = false
): IconBeat {
    val reduceMotion = rememberReduceMotion()
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    var armed by remember { mutableStateOf(playOnEnter) }

    LaunchedEffect(trigger, reduceMotion) {
        if (!armed) {
            armed = true
            return@LaunchedEffect
        }
        // Reduced motion keeps the colour change that accompanies the beat and drops the movement:
        // fewer and gentler, not nothing.
        if (reduceMotion) return@LaunchedEffect

        scale.snapTo(SetikerMotion.ICON_BEAT_SCALE)
        rotation.snapTo(SetikerMotion.ICON_BEAT_ROTATION_DEGREES)
        coroutineScope {
            launch { scale.animateTo(1f, SetikerMotion.DelightSpring) }
            launch { rotation.animateTo(0f, SetikerMotion.DelightSpring) }
        }
    }

    return IconBeat(scale = scale.asState(), rotationZ = rotation.asState())
}

/**
 * Applies [IconBeat] in the draw phase.
 *
 * Both values are read inside `graphicsLayer`, so the beat invalidates one layer per frame instead
 * of recomposing the glyph, the button and the bar around it.
 */
fun Modifier.iconBeat(beat: IconBeat): Modifier = graphicsLayer {
    scaleX = beat.scale.value
    scaleY = beat.scale.value
    rotationZ = beat.rotationZ.value
}
