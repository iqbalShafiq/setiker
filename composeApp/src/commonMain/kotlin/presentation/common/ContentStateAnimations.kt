package presentation.common

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntSize
import presentation.theme.SetikerMotion

/**
 * Subtle, case-specific content-phase transitions.
 * Prefer the named helper that matches the screen pattern — do not reuse one for all UI.
 *
 * The durations and curves come from [SetikerMotion] so every content swap in the app feels
 * like the same product.
 */
object ContentStateAnimations {
    const val ENTER_MS = SetikerMotion.DURATION_BASE_MS
    const val EXIT_MS = SetikerMotion.DURATION_FAST_MS
    const val SHEET_MS = SetikerMotion.DURATION_BASE_MS

    /**
     * A size transform that switches the container size immediately.
     *
     * `AnimatedContent` animates its own size by default, over 180 ms. Inside a `LazyColumn` or a
     * `LazyVerticalGrid` that means the list is re-measured on **every frame** of the swap, and the
     * screen stalls for the whole animation: measured on a Redmi Note 11, opening a screen dropped
     * to two frames and the app looked frozen for a fifth of a second. The content itself already
     * animates (fade and slide); the box around it should not.
     */
    fun noSizeAnimation(): SizeTransform =
        SizeTransform(clip = false, sizeAnimationSpec = { _: IntSize, _: IntSize -> snap() })

    /**
     * Builds a content transform whose container size switches immediately.
     *
     * `ContentTransform.using` is not reachable from Compose Multiplatform, so the transform is
     * built directly with the size transform attached.
     */
    fun contentTransform(
        enter: EnterTransition,
        exit: ExitTransition
    ): ContentTransform = ContentTransform(
        targetContentEnter = enter,
        initialContentExit = exit,
        targetContentZIndex = 0f,
        sizeTransform = noSizeAnimation()
    )

    /** List screens: loading / error / empty / content (Paywall, Blocked, Purchase history). */
    fun AnimatedContentTransitionScope<ListLoadPhase>.listLoad(): ContentTransform {
        val enteringContent = targetState == ListLoadPhase.Content
        val leavingContent = initialState == ListLoadPhase.Content
        return when {
            enteringContent ->
                contentTransform(fadeSlideUpIn(), fadeOut(animationSpec = tween(EXIT_MS)))
            leavingContent ->
                contentTransform(fadeIn(animationSpec = tween(ENTER_MS)), fadeSlideDownOut())
            else -> fadeSwap()
        }
    }

    /** Auth-style forms: spinner → editable fields (Edit profile). */
    fun AnimatedContentTransitionScope<Boolean>.formReveal(): ContentTransform {
        // true = loading, false = form ready
        return if (initialState && !targetState) {
            contentTransform(fadeSlideUpIn(), fadeOut(animationSpec = tween(EXIT_MS)))
        } else {
            fadeSwap()
        }
    }

    /** Profile / pack detail shells: full-screen phase swaps. */
    fun AnimatedContentTransitionScope<DetailLoadPhase>.detailReveal(): ContentTransform {
        return when {
            targetState == DetailLoadPhase.Ready ->
                contentTransform(fadeSlideUpIn(), fadeOut(animationSpec = tween(EXIT_MS)))
            initialState == DetailLoadPhase.Ready ->
                contentTransform(fadeIn(animationSpec = tween(ENTER_MS)), fadeSlideDownOut())
            else -> fadeSwap()
        }
    }

    /** Legal / long-form documents: softer fade into readable content. */
    fun AnimatedContentTransitionScope<DocumentLoadPhase>.documentReveal(): ContentTransform {
        return when {
            targetState == DocumentLoadPhase.Ready ->
                contentTransform(
                    fadeIn(animationSpec = tween(220)) +
                        slideInVertically(animationSpec = tween(220)) { it / 24 },
                    fadeOut(animationSpec = tween(EXIT_MS))
                )
            else -> fadeSwap()
        }
    }

    /** Explore list-body phases nested under search/tabs/featured chrome. */
    fun AnimatedContentTransitionScope<ExploreBodyPhase>.exploreBody(): ContentTransform {
        val enteringContent = targetState == ExploreBodyPhase.Content
        val leavingContent = initialState == ExploreBodyPhase.Content
        return when {
            enteringContent ->
                contentTransform(fadeSlideUpIn(), fadeOut(animationSpec = tween(EXIT_MS)))
            leavingContent ->
                contentTransform(fadeIn(animationSpec = tween(ENTER_MS)), fadeSlideDownOut())
            else -> fadeSwap()
        }
    }

    /** Sync / history result regions: fade only between nested phases. */
    fun fadeOnly(): ContentTransform = fadeSwap()

    private fun fadeSwap(): ContentTransform = contentTransform(
        fadeIn(animationSpec = tween(ENTER_MS, easing = SetikerMotion.EaseOut)),
        fadeOut(animationSpec = tween(EXIT_MS, easing = SetikerMotion.EaseOut))
    )

    private fun fadeSlideUpIn(): EnterTransition =
        fadeIn(animationSpec = tween(ENTER_MS, easing = SetikerMotion.EaseOut)) +
            slideInVertically(animationSpec = tween(ENTER_MS, easing = SetikerMotion.EaseOut)) { it / 16 }

    private fun fadeSlideDownOut(): ExitTransition =
        fadeOut(animationSpec = tween(EXIT_MS, easing = SetikerMotion.EaseOut)) +
            slideOutVertically(animationSpec = tween(EXIT_MS, easing = SetikerMotion.EaseOut)) { it / 20 }
}

enum class ListLoadPhase {
    Loading,
    Error,
    Empty,
    Content
}

enum class DetailLoadPhase {
    Loading,
    Failed,
    GuestEmpty,
    Ready
}

enum class DocumentLoadPhase {
    Loading,
    Error,
    Ready
}

enum class ExploreBodyPhase {
    SignIn,
    Loading,
    Error,
    Empty,
    Content
}

enum class ShareLinksBodyPhase {
    Loading,
    Empty,
    Links
}
