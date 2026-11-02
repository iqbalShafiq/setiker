package presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import presentation.common.rememberReduceMotion
import presentation.theme.SetikerMotion

/**
 * Scrollable bottom-sheet body with keyboard-aware insets.
 * Padding is applied after scroll so the last items are not clipped.
 *
 * The body fades and rises a few dp when the sheet opens. The rows can add their own small
 * stagger on top of this; the whole body moving first keeps the sheet from appearing as a static
 * block while the content is still being read.
 */
@Composable
fun BottomSheetScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    var revealed by remember { mutableStateOf(false) }
    val reveal by animateFloatAsState(
        targetValue = if (revealed || reduceMotion) 1f else 0f,
        animationSpec = SetikerMotion.out(SetikerMotion.DURATION_BASE_MS),
        label = "bottom_sheet_content_reveal"
    )
    LaunchedEffect(Unit) {
        revealed = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 8.dp.toPx()
            }
            .keyboardAwareScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        content = content
    )
}
