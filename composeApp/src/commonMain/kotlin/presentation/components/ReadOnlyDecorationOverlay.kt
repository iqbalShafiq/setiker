package presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.resolveEmojiStyle
import kotlin.math.roundToInt

/**
 * Renders persisted sticker decorations read-only (used for thumbnails and grid picks).
 * Layout matches [presentation.components.DecorationPreviewLayer].
 */
@Composable
fun ReadOnlyDecorationOverlay(
    decorations: List<StickerDecoration>,
    modifier: Modifier = Modifier
) {
    if (decorations.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val minDim = minOf(widthPx, heightPx)
        val density = LocalDensity.current

        decorations.forEach { decoration ->
            val scale = decoration.scale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)

            val textMetrics = if (decoration is TextDecoration) {
                rememberTextDecorationBoxMetrics(
                    decoration = decoration,
                    minDim = minDim,
                    canvasWidthPx = widthPx,
                    scale = scale
                )
            } else {
                null
            }

            val itemWidthPx: Float
            val itemHeightPx: Float
            when (decoration) {
                is TextDecoration -> {
                    itemWidthPx = textMetrics!!.widthPx
                    itemHeightPx = textMetrics.heightPx
                }
                is EmojiDecoration -> {
                    val sq = minDim * DecorationRenderSpec.EMOJI_BOX_RATIO * scale
                    itemWidthPx = sq
                    itemHeightPx = sq
                }
                is ImageDecoration -> {
                    val sq = minDim * DecorationRenderSpec.IMAGE_BASE_RATIO * scale
                    itemWidthPx = sq
                    itemHeightPx = sq
                }
            }

            val centerX = decoration.centerX.coerceIn(0f, 1f) * widthPx
            val centerY = decoration.centerY.coerceIn(0f, 1f) * heightPx
            val topLeft = Offset(
                x = centerX - itemWidthPx / 2f,
                y = centerY - itemHeightPx / 2f
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            topLeft.x.roundToInt(),
                            topLeft.y.roundToInt()
                        )
                    }
                    .size(
                        width = with(density) { itemWidthPx.toDp() },
                        height = with(density) { itemHeightPx.toDp() }
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                when (decoration) {
                    is TextDecoration -> {
                        DecorationTextContent(
                            decoration = decoration,
                            minDim = minDim,
                            scale = scale,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent),
                            fitContainer = true,
                            boxMetrics = textMetrics
                        )
                    }

                    is EmojiDecoration -> {
                        val emojiSizePx = minDim * DecorationRenderSpec.EMOJI_SIZE_RATIO * scale
                        StyledDecorationText(
                            text = decoration.emoji,
                            resolvedStyle = decoration.resolveEmojiStyle(emojiSizePx),
                            baseStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = with(density) { emojiSizePx.toSp() },
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is ImageDecoration -> {
                        Image(
                            painter = rememberAsyncImagePainter(decoration.imagePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Previews
@Preview
@Composable
private fun ReadOnlyDecorationOverlayPreview() {
    MaterialTheme {
        ReadOnlyDecorationOverlay(
            decorations = listOf(
                TextDecoration(
                    id = "txt_1",
                    text = "Hello",
                    font = DecorationFont.Sans,
                    fontWeight = DecorationFontWeight.SemiBold,
                    textColorArgb = 0xFF000000L,
                    centerX = 0.5f,
                    centerY = 0.3f,
                    scale = 1f
                ),
                EmojiDecoration(
                    id = "emoji_1",
                    emoji = "😎",
                    centerX = 0.7f,
                    centerY = 0.6f,
                    scale = 1.2f
                )
            ),
            modifier = Modifier.size(200.dp)
        )
    }
}
