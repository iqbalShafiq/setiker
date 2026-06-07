package presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

object NavTransitionDefaults {
    const val DURATION_MS = 300
}

const val NAV_FORWARD_TRANSITION_KEY = "nav_forward_transition"

enum class NavForwardTransition {
    /** New screen slides up from the bottom (default push). */
    BOTTOM_UP,
    /** New screen slides down from the top (top app bar actions & modal-style screens). */
    TOP_DOWN
}

/**
 * Coordinates forward and pop navigation animations across the [NavHost].
 *
 * Forward direction is chosen per navigation via [navigateBottomUp] / [navigateTopDown].
 * The chosen style is persisted on each back-stack entry so [popEnterTransition] and
 * [popExitTransition] reverse the exact animation that was used to open that screen.
 */
class NavigationTransitionController {
    private var pendingForwardStyle: NavForwardTransition = NavForwardTransition.BOTTOM_UP

    fun bindTo(navController: NavHostController): () -> Unit {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val entry = navController.currentBackStackEntry ?: return@OnDestinationChangedListener
            if (entry.savedStateHandle.get<String>(NAV_FORWARD_TRANSITION_KEY) == null) {
                entry.saveEntryTransitionStyle(resolveForwardStyle(destination.route))
            }
        }
        navController.addOnDestinationChangedListener(listener)
        return { navController.removeOnDestinationChangedListener(listener) }
    }

    fun enterTransition(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): EnterTransition {
        return when (resolveForwardStyle(scope.targetState.destination.route)) {
            NavForwardTransition.BOTTOM_UP -> bottomUpEnter()
            NavForwardTransition.TOP_DOWN -> topDownEnter()
        }
    }

    fun exitTransition(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): ExitTransition {
        return when (resolveForwardStyle(scope.targetState.destination.route)) {
            NavForwardTransition.BOTTOM_UP -> bottomUpSourceExit()
            NavForwardTransition.TOP_DOWN -> topDownSourceExit()
        }
    }

    fun popEnterTransition(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): EnterTransition {
        return when (scope.initialState.entryTransitionStyle()) {
            NavForwardTransition.BOTTOM_UP -> bottomUpPopEnter()
            NavForwardTransition.TOP_DOWN -> topDownPopEnter()
        }
    }

    fun popExitTransition(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): ExitTransition {
        return when (scope.initialState.entryTransitionStyle()) {
            NavForwardTransition.BOTTOM_UP -> bottomUpPopExit()
            NavForwardTransition.TOP_DOWN -> topDownPopExit()
        }
    }

    fun navigateBottomUp(
        navController: NavHostController,
        route: String,
        builder: NavOptionsBuilder.() -> Unit = {}
    ) {
        pendingForwardStyle = NavForwardTransition.BOTTOM_UP
        navController.navigate(route, builder)
    }

    fun navigateTopDown(
        navController: NavHostController,
        route: String,
        builder: NavOptionsBuilder.() -> Unit = {}
    ) {
        pendingForwardStyle = NavForwardTransition.TOP_DOWN
        navController.navigate(route, builder)
    }

    private fun resolveForwardStyle(targetRoute: String?): NavForwardTransition {
        if (targetRoute != null && targetRoute.isAlwaysTopDownDestination()) {
            return NavForwardTransition.TOP_DOWN
        }
        return pendingForwardStyle
    }
}

fun NavBackStackEntry.entryTransitionStyle(): NavForwardTransition {
    val stored = savedStateHandle.get<String>(NAV_FORWARD_TRANSITION_KEY) ?: return NavForwardTransition.BOTTOM_UP
    return runCatching { NavForwardTransition.valueOf(stored) }
        .getOrDefault(NavForwardTransition.BOTTOM_UP)
}

fun NavBackStackEntry.saveEntryTransitionStyle(style: NavForwardTransition) {
    savedStateHandle[NAV_FORWARD_TRANSITION_KEY] = style.name
}

/**
 * Destinations that always enter from the top regardless of the navigation call site
 * (modal editors, share deep links, etc.). Multi-entry screens such as [aiJobs] are
 * intentionally excluded so their pop animation follows how they were opened.
 */
fun String.isAlwaysTopDownDestination(): Boolean = when {
    startsWith("crop/") -> true
    startsWith("videoTrim/") -> true
    startsWith("videoCrop/") -> true
    startsWith("sharePreview/") -> true
    else -> false
}

private fun slideAnimationSpec() = tween<IntOffset>(NavTransitionDefaults.DURATION_MS)
private fun fadeAnimationSpec() = tween<Float>(NavTransitionDefaults.DURATION_MS)

private fun bottomUpEnter(): EnterTransition =
    slideInVertically(initialOffsetY = { fullHeight -> fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeIn(animationSpec = fadeAnimationSpec())

private fun topDownEnter(): EnterTransition =
    slideInVertically(initialOffsetY = { fullHeight -> -fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeIn(animationSpec = fadeAnimationSpec())

private fun bottomUpSourceExit(): ExitTransition =
    slideOutVertically(targetOffsetY = { fullHeight -> -fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeOut(animationSpec = fadeAnimationSpec())

private fun topDownSourceExit(): ExitTransition =
    slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeOut(animationSpec = fadeAnimationSpec())

/** Reverse of [bottomUpEnter]: screen exits downward. */
private fun bottomUpPopExit(): ExitTransition =
    slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeOut(animationSpec = fadeAnimationSpec())

/** Reverse of [topDownEnter]: screen exits upward. */
private fun topDownPopExit(): ExitTransition =
    slideOutVertically(targetOffsetY = { fullHeight -> -fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeOut(animationSpec = fadeAnimationSpec())

/** Reverse of [bottomUpSourceExit]: previous screen re-enters from the top. */
private fun bottomUpPopEnter(): EnterTransition =
    slideInVertically(initialOffsetY = { fullHeight -> -fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeIn(animationSpec = fadeAnimationSpec())

/** Reverse of [topDownSourceExit]: previous screen re-enters from the bottom. */
private fun topDownPopEnter(): EnterTransition =
    slideInVertically(initialOffsetY = { fullHeight -> fullHeight }, animationSpec = slideAnimationSpec()) +
        fadeIn(animationSpec = fadeAnimationSpec())
