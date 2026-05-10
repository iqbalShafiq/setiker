package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/**
 * Square sticker preview frame used across Editor, animated editor, and video trim.
 * Matches the app neubrutal card: offset shadow, thick border, theme-aware fill so
 * it adapts to both light and dark mode automatically.
 */
@Composable
fun NeubrutalStickerPreviewFrame(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    shadowOffsetX: Dp = 4.dp,
    shadowOffsetY: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .neubrutalShadow(
                offsetX = shadowOffsetX,
                offsetY = shadowOffsetY,
                cornerRadius = cornerRadius,
                color = shadow
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(surface)
            .border(
                width = 2.dp,
                color = border,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}
