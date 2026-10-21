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
 * - Movement that happens on screen uses **ease-in-out**.
 * - Press feedback uses a **non-bouncy** spring: buttons are pressed many times a day and a
 *   wobble reads as sloppiness rather than personality. Bounce is reserved for rare, celebratory
 *   moments.
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

    /** Strong ease-out for entrances and exits. */
    val EaseOut: CubicBezierEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** Strong ease-in-out for movement that happens on screen. */
    val EaseInOut: CubicBezierEasing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

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

    /** Standard tween using [EaseInOut]. */
    fun <T> inOut(durationMillis: Int = DURATION_BASE_MS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EaseInOut)

    /** Standard tween using [EaseStandard]. */
    fun <T> standard(durationMillis: Int = DURATION_BASE_MS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EaseStandard)
}
