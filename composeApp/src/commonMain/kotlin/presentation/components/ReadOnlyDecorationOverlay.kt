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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.roundToInt

/**
 * Renders persisted sticker decorations read-only (used for thumbnails and grid picks).
 * Layout matches [presentation.components.DecorationPreviewLayer] (including api_txt caption geometry).
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

            val itemWidthPx: Float
            val itemHeightPx: Float
            when (decoration) {
                is TextDecoration -> {
                    if (decoration.id.startsWith("api_txt_")) {
                        itemWidthPx = widthPx * (1f - 2f * DecorationRenderSpec.API_CAPTION_HORIZONTAL_INSET_RATIO)
                        itemHeightPx = minDim * 0.46f * scale
                    } else {
                        val sq = minDim * DecorationRenderSpec.TEXT_BOX_RATIO * scale
                        itemWidthPx = sq
                        itemHeightPx = sq
                    }
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
                        val textRatio = if (decoration.id.startsWith("api_txt_")) {
                            DecorationRenderSpec.API_CAPTION_TEXT_SIZE_RATIO
                        } else {
                            DecorationRenderSpec.TEXT_SIZE_RATIO
                        }
                        Text(
                            text = decoration.text,
                            style = TextStyle(
                                color = Color(decoration.textColorArgb.toInt()),
                                fontFamily = mapDecorationFont(decoration.font),
                                fontWeight = mapDecorationFontWeight(decoration.fontWeight),
                                fontSize = with(density) {
                                    (minDim * textRatio * scale).toSp()
                                },
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent),
                            maxLines = if (decoration.id.startsWith("api_txt_")) 6 else 3
                        )
                    }

                    is EmojiDecoration -> {
                        Text(
                            text = decoration.emoji,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = with(density) {
                                    (minDim * DecorationRenderSpec.EMOJI_SIZE_RATIO * scale).toSp()
                                }
                            ),
                            textAlign = TextAlign.Center,
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

private fun mapDecorationFont(font: DecorationFont): FontFamily = when (font) {
    DecorationFont.Sans -> FontFamily.SansSerif
    DecorationFont.Serif -> FontFamily.Serif
    DecorationFont.Mono -> FontFamily.Monospace
    DecorationFont.Cursive -> FontFamily.Cursive
    DecorationFont.Display -> FontFamily.Serif
    DecorationFont.Rounded -> FontFamily.SansSerif
    DecorationFont.Condensed -> FontFamily.SansSerif
}

private fun mapDecorationFontWeight(weight: DecorationFontWeight): FontWeight = when (weight) {
    DecorationFontWeight.Light -> FontWeight.Light
    DecorationFontWeight.Regular -> FontWeight.Normal
    DecorationFontWeight.Medium -> FontWeight.Medium
    DecorationFontWeight.SemiBold -> FontWeight.SemiBold
    DecorationFontWeight.Bold -> FontWeight.Bold
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
