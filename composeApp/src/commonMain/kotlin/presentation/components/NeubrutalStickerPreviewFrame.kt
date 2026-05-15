package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
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
    cornerRadius: Dp = NeubrutalCardRadius,
    shadowOffsetX: Dp = NeubrutalShadowOffset,
    shadowOffsetY: Dp = NeubrutalShadowOffset,
    content: @Composable BoxScope.() -> Unit
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(cornerRadius)

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
            .clip(shape)
            .background(surface)
            .border(
                width = NeubrutalBorderWidth,
                color = border,
                shape = shape
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// MARK: - Previews

@Preview
@Composable
private fun NeubrutalStickerPreviewFramePreview() {
    MaterialTheme {
        NeubrutalStickerPreviewFrame {
            Text("Sticker")
        }
    }
}
