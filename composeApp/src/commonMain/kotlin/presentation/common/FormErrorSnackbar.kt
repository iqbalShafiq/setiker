package presentation.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import presentation.common.UiText
import presentation.common.resolveLocal
import presentation.components.AppSnackbarHostState

/**
 * The form error, shown in the snackbar.
 *
 * The error used to be a card at the bottom of the form, which is exactly where the user is not
 * looking after they press the button at the bottom of the screen. The snackbar appears where the
 * thumb is, it says what went wrong in the danger treatment, and it announces itself to a screen
 * reader. The line that drains along its bottom edge is the time the user has before it closes.
 *
 * The snackbar stays until the timer runs out or the user closes it; the error itself stays in
 * the state, so the screen can show it again the next time the request fails.
 */
@Composable
internal fun rememberErrorSnackbar(error: UiText?): AppSnackbarHostState {
    val hostState = remember { AppSnackbarHostState() }
    // The message is resolved in the composition: a string resource cannot be read from inside
    // the effect.
    val message = error?.resolveLocal()

    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        // A second failure replaces the first message instead of queueing behind it.
        hostState.hostState.currentSnackbarData?.dismiss()
        hostState.showError(message)
    }

    return hostState
}
