package presentation.home

import domain.model.AuthState

/**
 * What the home bottom bar should do when the user taps an account action.
 */
internal enum class HomeAuthAction {
    /** The user is signed in: open the signed-in destination. */
    SIGNED_IN,

    /** The user is signed out: open the login screen. */
    SIGN_IN,

    /** The session has not been resolved yet: do nothing, and keep the control disabled. */
    WAITING
}

/**
 * Resolves the account action for the home bottom bar.
 *
 * The distinction between [AuthState.UNKNOWN] and [AuthState.UNAUTHENTICATED] matters: on a cold
 * start the session is resolved asynchronously, and treating "not resolved yet" as "signed out"
 * sent signed-in users to the login screen.
 */
internal fun homeAuthAction(authState: AuthState): HomeAuthAction = when (authState) {
    AuthState.AUTHENTICATED -> HomeAuthAction.SIGNED_IN
    AuthState.UNAUTHENTICATED -> HomeAuthAction.SIGN_IN
    AuthState.UNKNOWN -> HomeAuthAction.WAITING
}
