package presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Bottom insets for scrollable content when the IME is visible.
 *
 * Navigation-bar padding is excluded while the IME is open (same rule as
 * [packBottomBarOuterPadding]) so content and bottom bars do not show an extra gap
 * above the keyboard.
 */
@Composable
fun Modifier.keyboardAwareInsets(): Modifier =
    windowInsetsPadding(
        WindowInsets.navigationBars
            .exclude(WindowInsets.ime)
            .only(WindowInsetsSides.Bottom)
    )
        .imePadding()
