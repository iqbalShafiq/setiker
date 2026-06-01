package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalOnSurface

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    onPageClick: ((Int) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val dotColor = if (selected) {
                AccentCoral
            } else {
                neubrutalOnSurface().copy(alpha = 0.18f)
            }
            val dotModifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(
                    width = 1.dp,
                    color = if (selected) neubrutalBorderColor() else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                    shape = CircleShape
                )

            Box(
                modifier = if (onPageClick != null) {
                    Modifier
                        .size(32.dp)
                        .clickable { onPageClick(index) }
                } else {
                    Modifier.size(32.dp)
                },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = dotModifier)
            }
        }
    }
}
