package presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The app's snackbar state.
 *
 * Material's own state says nothing about what kind of message it is showing, so a host cannot
 * tell a failure from a confirmation. The kind lives here, next to the message it belongs to,
 * which is the only way it can be right: a screen that shows a confirmation and a failure one
 * after the other would otherwise have to guess from its own state, and the guess is wrong the
 * moment the state changes while the message is still on screen.
 */
class AppSnackbarHostState {

    private val _hostState = SnackbarHostState()

    /** The state the host renders. */
    val hostState: SnackbarHostState get() = _hostState

    /** Whether the message on screen is a failure. */
    var isError by mutableStateOf(false)
        private set

    /**
     * Shows a confirmation.
     *
     * A confirmation is a note: it gets the plain treatment and a shorter stay.
     */
    suspend fun showMessage(message: String, actionLabel: String? = null) {
        isError = false
        _hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Indefinite
        )
    }

    /**
     * Shows a failure.
     *
     * A failure gets the danger treatment and stays longer, because it is the one message in the
     * app the user has to act on.
     */
    suspend fun showError(message: String, actionLabel: String? = null) {
        isError = true
        _hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Indefinite
        )
    }
}
