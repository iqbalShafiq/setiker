package presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Scrollable bottom-sheet body. Padding is applied after [verticalScroll] so the last
 * items are not clipped by the sheet's inset padding (clip-to-padding behavior).
 */
@Composable
fun BottomSheetScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        content = content
    )
}

/** Disable default sheet insets so scroll padding controls the bottom gap. */
fun zeroBottomSheetWindowInsets(): WindowInsets = WindowInsets(0, 0, 0, 0)
