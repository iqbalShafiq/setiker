package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The app's small vocabulary of haptics.
 *
 * Neubrutalism is a tactile visual language, but the app had no tactile feedback at all: a save,
 * a selection and a long-press all felt the same. These are intentionally few and short; a phone
 * that buzzes on every tap is worse than one that never does.
 */
@Stable
class AppHaptics(private val feedback: HapticFeedback) {

    /** A light tick for a selection or a small state change. */
    fun selection() {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** A firm confirmation for a completed action (save, publish, follow). */
    fun confirm() {
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** A long-press that opened a secondary action. */
    fun longPress() {
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { AppHaptics(feedback) }
}
