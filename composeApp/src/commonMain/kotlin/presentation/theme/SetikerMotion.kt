package presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * The single source of truth for motion in the app.
 *
 * Before this file every animation site invented its own duration and easing, so the same
 * interaction could feel different from one screen to the next. Anything that moves should pick
 * its values from here.
 *
 * Guidelines (see the motion audit):
 * - Entrances and exits use **ease-out**: they start fast, which is what the eye is waiting for.
 * - Colour and opacity changes use the gentler [EaseStandard].
 * - Press feedback uses a **non-bouncy** spring: buttons are pressed many times a day and a
 *   wobble reads as sloppiness rather than personality. Bounce is reserved for rare, celebratory
 *   moments.
 * - On-screen movement is driven by that same spring (drag, reorder, press), so there is no
 *   separate ease-in-out curve to drift out of use.
 * - UI animations stay below 300 ms.
 */
object SetikerMotion {

    /** Press feedback, chips, small affordances. */
    const val DURATION_FAST_MS = 140

    /** Default content swap: loading → content, tab changes. */
    const val DURATION_BASE_MS = 180

    /** Sheets and larger surfaces. */
    const val DURATION_SLOW_MS = 260

    /** Full screen transitions. */
    const val DURATION_SCREEN_MS = 300

    /** Long-form documents: a softer reveal than a list, still under the 300 ms ceiling. */
    const val DURATION_DOCUMENT_MS = 220

    /** The branded loader: one bounce and the pause between bounces. */
    const val DURATION_LOADER_MS = 300

    /** The delay between the three loader dots starting their bounce. */
    const val LOADER_STAGGER_MS = 150

    /** The pause between loader bounce cycles. */
    const val LOADER_HOLD_MS = 300

    /** The image shimmer band. Slow enough to read as light, not as a progress bar. */
    const val DURATION_SHIMMER_MS = 1100

    /** How long a celebration badge stays on screen after it lands. */
    const val CELEBRATION_HOLD_MS = 1200

    /**
     * Press scales.
     *
     * They are deliberately few: a press should read as the same gesture whether it happens on
     * a 56 dp FAB or a 28 dp chip. The value only changes with the size of the control, not with
     * the screen it lives on.
     */
    const val PRESS_SCALE_TINY = 0.88f
    const val PRESS_SCALE_SMALL = 0.92f
    const val PRESS_SCALE_COMPACT = 0.95f
    const val PRESS_SCALE_REGULAR = 0.97f
    const val PRESS_SCALE_SUBTLE = 0.98f

    /** A stagger step for revealing a short list inside a sheet. */
    const val STAGGER_STEP_MS = 40

    /** Strong ease-out for entrances and exits. */
    val EaseOut: CubicBezierEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** Neutral curve for colour and opacity changes. */
    val EaseStandard: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * Press feedback spring: firm, no wobble.
     *
     * The previous `DampingRatioMediumBouncy` made every button overshoot on release, which was
     * measurable on device (a pressed FAB went 127 px → 117 px → 119 px at 10× speed).
     */
    val PressSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Celebration spring: a single, deliberate bounce.
     *
     * Only for rare, high-emotion moments (a sticker pack was created, a save completed).
     */
    val DelightSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Standard tween using [EaseOut]. */
    fun <T> out(durationMillis: Int = DURATION_BASE_MS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EaseOut)

    /** Standard tween using [EaseStandard]. */
    fun <T> standard(durationMillis: Int = DURATION_BASE_MS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EaseStandard)
}
