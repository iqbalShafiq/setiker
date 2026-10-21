package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.ScreenContentHorizontalPadding
import presentation.theme.breakOutOfParentHorizontalPadding
import presentation.theme.neubrutalScreenBackground

/**
 * Edge-to-edge horizontally scrollable row inside a parent with [presentation.theme.screenContentHorizontalPadding].
 * The first item aligns with sibling content; chips can scroll past the screen edge.
 *
 * While there is more to scroll, the trailing edge fades into the page. Without it a chip that
 * runs past the screen edge looked broken rather than scrollable.
 */
@Composable
fun ScreenContentHorizontalScrollRow(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ScreenContentHorizontalPadding,
    contentSpacing: Dp = 8.dp,
    content: @Composable RowScope.(contentSpacing: Dp) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .breakOutOfParentHorizontalPadding(horizontalPadding)
                .horizontalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.width(horizontalPadding))
            content(contentSpacing)
            Spacer(modifier = Modifier.width(horizontalPadding))
        }

        if (scrollState.canScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(28.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, neubrutalScreenBackground())
                        )
                    )
            )
        }
    }
}
