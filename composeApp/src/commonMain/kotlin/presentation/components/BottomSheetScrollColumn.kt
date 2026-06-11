package presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Scrollable bottom-sheet body with keyboard-aware insets.
 * Padding is applied after scroll so the last items are not clipped.
 */
@Composable
fun BottomSheetScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .keyboardAwareScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        content = content
    )
}

/** Disable default sheet insets so [BottomSheetScrollColumn] controls the bottom gap. */
fun zeroBottomSheetWindowInsets(): WindowInsets = WindowInsets(0, 0, 0, 0)
