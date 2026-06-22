package presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.ScreenContentHorizontalPadding
import presentation.theme.breakOutOfParentHorizontalPadding

/**
 * Edge-to-edge horizontally scrollable row inside a parent with [presentation.theme.screenContentHorizontalPadding].
 * The first item aligns with sibling content; chips can scroll past the screen edge.
 */
@Composable
fun ScreenContentHorizontalScrollRow(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ScreenContentHorizontalPadding,
    contentSpacing: Dp = 8.dp,
    content: @Composable RowScope.(contentSpacing: Dp) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .breakOutOfParentHorizontalPadding(horizontalPadding)
            .horizontalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.width(horizontalPadding))
        content(contentSpacing)
        Spacer(modifier = Modifier.width(horizontalPadding))
    }
}
