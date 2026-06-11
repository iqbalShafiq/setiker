package presentation.components

import androidx.compose.ui.Modifier

/**
 * Bottom insets for scrollable content when the IME is visible.
 *
 * Pair with [keyboardAwareScroll] / [keyboardAwareLazyList] on the scroll container —
 * not on individual text fields. Android relies on `adjustNothing` + WindowInsets;
 * iOS uses the same modifier chain.
 */
expect fun Modifier.keyboardAwareInsets(): Modifier
