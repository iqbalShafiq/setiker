package presentation.home

import domain.model.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A signed-in user who taps "Sync" or "Profile" in the first seconds after a cold start used to
 * be bounced to the login screen: the session had not been resolved yet, so the home screen saw
 * "no user" and assumed "signed out".
 *
 * Reproduced 3 out of 3 times on a cold start.
 */
class HomeAuthActionTest {

    @Test
    fun `an unresolved session must not send the user to the login screen`() {
        assertEquals(HomeAuthAction.WAITING, homeAuthAction(AuthState.UNKNOWN))
    }

    @Test
    fun `a signed out user is sent to the login screen`() {
        assertEquals(HomeAuthAction.SIGN_IN, homeAuthAction(AuthState.UNAUTHENTICATED))
    }

    @Test
    fun `a signed in user gets the signed in action`() {
        assertEquals(HomeAuthAction.SIGNED_IN, homeAuthAction(AuthState.AUTHENTICATED))
    }
}
