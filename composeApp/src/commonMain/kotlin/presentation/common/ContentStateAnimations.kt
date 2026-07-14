package presentation.common

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Subtle, case-specific content-phase transitions.
 * Prefer the named helper that matches the screen pattern — do not reuse one for all UI.
 */
object ContentStateAnimations {
    const val ENTER_MS = 180
    const val EXIT_MS = 120
    const val SHEET_MS = 160

    /** List screens: loading / error / empty / content (Paywall, Blocked, Purchase history). */
    fun AnimatedContentTransitionScope<ListLoadPhase>.listLoad(): ContentTransform {
        val enteringContent = targetState == ListLoadPhase.Content
        val leavingContent = initialState == ListLoadPhase.Content
        return when {
            enteringContent -> fadeSlideUpIn() togetherWith fadeOut(animationSpec = tween(EXIT_MS))
            leavingContent -> fadeIn(animationSpec = tween(ENTER_MS)) togetherWith fadeSlideDownOut()
            else -> fadeSwap()
        }
    }

    /** Auth-style forms: spinner → editable fields (Edit profile). */
    fun AnimatedContentTransitionScope<Boolean>.formReveal(): ContentTransform {
        // true = loading, false = form ready
        return if (initialState && !targetState) {
            fadeSlideUpIn() togetherWith fadeOut(animationSpec = tween(EXIT_MS))
        } else {
            fadeSwap()
        }
    }

    /** Profile / pack detail shells: full-screen phase swaps. */
    fun AnimatedContentTransitionScope<DetailLoadPhase>.detailReveal(): ContentTransform {
        return when {
            targetState == DetailLoadPhase.Ready ->
                fadeSlideUpIn() togetherWith fadeOut(animationSpec = tween(EXIT_MS))
            initialState == DetailLoadPhase.Ready ->
                fadeIn(animationSpec = tween(ENTER_MS)) togetherWith fadeSlideDownOut()
            else -> fadeSwap()
        }
    }

    /** Legal / long-form documents: softer fade into readable content. */
    fun AnimatedContentTransitionScope<DocumentLoadPhase>.documentReveal(): ContentTransform {
        return when {
            targetState == DocumentLoadPhase.Ready ->
                fadeIn(animationSpec = tween(220)) +
                    slideInVertically(animationSpec = tween(220)) { it / 24 } togetherWith
                    fadeOut(animationSpec = tween(EXIT_MS))
            else -> fadeSwap()
        }
    }

    /** Explore list-body phases nested under search/tabs/featured chrome. */
    fun AnimatedContentTransitionScope<ExploreBodyPhase>.exploreBody(): ContentTransform {
        val enteringContent = targetState == ExploreBodyPhase.Content
        val leavingContent = initialState == ExploreBodyPhase.Content
        return when {
            enteringContent -> fadeSlideUpIn() togetherWith fadeOut(animationSpec = tween(EXIT_MS))
            leavingContent -> fadeIn(animationSpec = tween(ENTER_MS)) togetherWith fadeSlideDownOut()
            else -> fadeSwap()
        }
    }

    /** Sync / history result regions: fade only between nested phases. */
    fun fadeOnly(): ContentTransform = fadeSwap()

    private fun fadeSwap(): ContentTransform =
        fadeIn(animationSpec = tween(ENTER_MS)) togetherWith fadeOut(animationSpec = tween(EXIT_MS))

    private fun fadeSlideUpIn() =
        fadeIn(animationSpec = tween(ENTER_MS)) +
            slideInVertically(animationSpec = tween(ENTER_MS)) { it / 16 }

    private fun fadeSlideDownOut() =
        fadeOut(animationSpec = tween(EXIT_MS)) +
            slideOutVertically(animationSpec = tween(EXIT_MS)) { it / 20 }
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
