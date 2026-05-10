package presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 */
@Composable
fun ReadOnlyDecorationOverlay(
    decorations: List<StickerDecoration>,
    modifier: Modifier = Modifier
) {
    if (decorations.isEmpty()) return

    val apiCaptions = decorations.filterIsInstance<TextDecoration>()
        .filter { it.id.startsWith("api_txt_") }
    val rest = decorations.filter { dec ->
        when (dec) {
            is TextDecoration -> !dec.id.startsWith("api_txt_")
            else -> true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val minDim = minOf(widthPx, heightPx)

        if (apiCaptions.isNotEmpty()) {
            ApiOutsideForegroundCaptionOverlay(
                captions = apiCaptions,
                widthPx = widthPx,
                heightPx = heightPx,
                minDim = minDim,
                modifier = Modifier.fillMaxSize()
            )
        }

        val density = LocalDensity.current

        rest.forEach { decoration ->
            val scale = decoration.scale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)
            val itemSizePx = when (decoration) {
                is TextDecoration -> minDim * DecorationRenderSpec.TEXT_BOX_RATIO * scale
                is EmojiDecoration -> minDim * DecorationRenderSpec.EMOJI_BOX_RATIO * scale
                is ImageDecoration -> minDim * DecorationRenderSpec.IMAGE_BASE_RATIO * scale
            }
            val centerX = decoration.centerX.coerceIn(0f, 1f) * widthPx
            val centerY = decoration.centerY.coerceIn(0f, 1f) * heightPx
            val topLeft = Offset(
                x = centerX - itemSizePx / 2f,
                y = centerY - itemSizePx / 2f
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
                        width = with(density) { itemSizePx.toDp() },
                        height = with(density) { itemSizePx.toDp() }
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                when (decoration) {
                    is TextDecoration -> {
                        Text(
                            text = decoration.text,
                            style = TextStyle(
                                color = Color(decoration.textColorArgb.toInt()),
                                fontFamily = mapDecorationFont(decoration.font),
                                fontWeight = mapDecorationFontWeight(decoration.fontWeight),
                                fontSize = with(density) {
                                    (minDim * DecorationRenderSpec.TEXT_SIZE_RATIO * scale).toSp()
                                },
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent),
                            maxLines = 3
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

@Composable
private fun ApiOutsideForegroundCaptionOverlay(
    captions: List<TextDecoration>,
    widthPx: Float,
    heightPx: Float,
    minDim: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val horizontalPadPx = widthPx * DecorationRenderSpec.API_CAPTION_HORIZONTAL_INSET_RATIO
    val bottomPadPx = heightPx * DecorationRenderSpec.API_CAPTION_BOTTOM_INSET_RATIO
    val maxCaptionWidthDp = with(density) {
        (widthPx - 2f * horizontalPadPx).coerceAtLeast(1f).toDp()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = with(density) { horizontalPadPx.toDp() },
                    end = with(density) { horizontalPadPx.toDp() },
                    bottom = with(density) { bottomPadPx.toDp() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            captions.forEach { decoration ->
                val scale = decoration.scale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)
                Text(
                    text = decoration.text,
                    style = TextStyle(
                        color = Color(decoration.textColorArgb.toInt()),
                        fontFamily = mapDecorationFont(decoration.font),
                        fontWeight = mapDecorationFontWeight(decoration.fontWeight),
                        fontSize = with(density) {
                            (minDim * DecorationRenderSpec.API_CAPTION_TEXT_SIZE_RATIO * scale).toSp()
                        },
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .widthIn(max = maxCaptionWidthDp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    maxLines = 6,
                    softWrap = true
                )
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
