package presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Bottom inset for [PackBottomBar] and wrappers ([HomeBottomBar], [MediaPreviewBottomBar], etc.).
 *
 * Applies gesture-navigation padding only when the IME is hidden. When the keyboard is
 * visible, [imePadding] on the bar already lifts it — excluding IME here avoids the extra
 * peach gap between the bar and the keyboard.
 */
@Composable
fun Modifier.packBottomBarOuterPadding(): Modifier =
    windowInsetsPadding(
        WindowInsets.navigationBars
            .exclude(WindowInsets.ime)
            .only(WindowInsetsSides.Bottom)
    )
