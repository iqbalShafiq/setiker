package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalDark
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor

/**
 * Neubrutal app logo mark matching the launcher icon (offset stacked squares).
 */
@Composable
fun AppLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = neubrutalBorderColor()
    val tileShape = RoundedCornerShape(NeubrutalCardRadius)
    val tileSize = size * 0.74f
    val shadowOffset = size * 0.08f

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(tileShape)
                .background(if (isDark) NeubrutalWhite else NeubrutalBlack)
                .border(width = 2.dp, color = borderColor, shape = tileShape)
        )

        Box(
            modifier = Modifier
                .size(tileSize)
                .clip(tileShape)
                .background(if (isDark) NeubrutalDark else NeubrutalBg)
                .border(width = 2.dp, color = borderColor, shape = tileShape)
        )
    }
}
