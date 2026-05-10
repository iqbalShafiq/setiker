package presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.DecorationFontWeight
import kotlin.math.roundToInt

@Composable
fun DecorationPreviewLayer(
    decorations: List<StickerDecoration>,
    selectedDecorationId: String?,
    onSelectDecoration: (String?) -> Unit,
    onUpdateDecoration: (id: String, centerX: Float, centerY: Float, scale: Float) -> Unit,
    onDeleteDecoration: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onSelectDecoration(null) })
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val minDim = minOf(widthPx, heightPx)
        val density = LocalDensity.current

        decorations.forEach { decoration ->
            val latestCenterX by rememberUpdatedState(decoration.centerX)
            val latestCenterY by rememberUpdatedState(decoration.centerY)
            val latestScale by rememberUpdatedState(decoration.scale)
            val scale = decoration.scale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)
            val itemSizePx = when (decoration) {
                is TextDecoration -> minDim * DecorationRenderSpec.TEXT_BOX_RATIO * scale
                is EmojiDecoration -> minDim * DecorationRenderSpec.EMOJI_BOX_RATIO * scale
                is ImageDecoration -> minDim * DecorationRenderSpec.IMAGE_BASE_RATIO * scale
            }

            val itemWidth = when (decoration) {
                is TextDecoration ->
                    if (decoration.id.startsWith("api_txt_")) {
                        widthPx * (1f - 2f * DecorationRenderSpec.API_CAPTION_HORIZONTAL_INSET_RATIO)
                    } else {
                        itemSizePx
                    }
                else -> itemSizePx
            }
            val itemHeight = when (decoration) {
                is TextDecoration ->
                    if (decoration.id.startsWith("api_txt_")) {
                        minDim * 0.46f * scale
                    } else {
                        itemSizePx
                    }
                else -> itemSizePx
            }
            val centerX = decoration.centerX.coerceIn(0f, 1f) * widthPx
            val centerY = decoration.centerY.coerceIn(0f, 1f) * heightPx
            val topLeft = Offset(
                x = centerX - itemWidth / 2f,
                y = centerY - itemHeight / 2f
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
                        width = with(density) { itemWidth.toDp() },
                        height = with(density) { itemHeight.toDp() }
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(decoration.id) {
                        var startCenterX = decoration.centerX
                        var startCenterY = decoration.centerY
                        var fixedScale = decoration.scale
                        detectDragGestures(
                            onDragStart = {
                                startCenterX = latestCenterX
                                startCenterY = latestCenterY
                                fixedScale = latestScale
                                onSelectDecoration(decoration.id)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            startCenterX += dragAmount.x / widthPx
                            startCenterY += dragAmount.y / heightPx
                            val newCenterX = startCenterX.coerceIn(0f, 1f)
                            val newCenterY = startCenterY.coerceIn(0f, 1f)
                            onUpdateDecoration(
                                decoration.id,
                                newCenterX,
                                newCenterY,
                                fixedScale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)
                            )
                        }
                    }
                    .pointerInput(decoration.id) {
                        detectTapGestures(onTap = { onSelectDecoration(decoration.id) })
                    }
                    .then(
                        Modifier.border(
                            width = 2.dp,
                            color = if (selectedDecorationId == decoration.id) Color.Black else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                    )
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
                                fontFamily = mapFontFamily(decoration.font),
                                fontWeight = mapFontWeight(decoration.fontWeight),
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

                if (selectedDecorationId == decoration.id) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .border(2.dp, Color.Black, CircleShape)
                            .pointerInput(decoration.id) {
                                detectTapGestures(onTap = { onDeleteDecoration(decoration.id) })
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    val handleSize = 20.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(handleSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color.Black, CircleShape)
                            .pointerInput(decoration.id) {
                                var startScale = decoration.scale
                                var fixedCenterX = decoration.centerX
                                var fixedCenterY = decoration.centerY
                                detectDragGestures(
                                    onDragStart = {
                                        startScale = latestScale
                                        fixedCenterX = latestCenterX
                                        fixedCenterY = latestCenterY
                                        onSelectDecoration(decoration.id)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    startScale += (dragAmount.x + dragAmount.y) / minDim
                                    val newScale = startScale.coerceIn(
                                        DecorationRenderSpec.MIN_SCALE,
                                        DecorationRenderSpec.MAX_SCALE
                                    )
                                    onUpdateDecoration(
                                        decoration.id,
                                        fixedCenterX.coerceIn(0f, 1f),
                                        fixedCenterY.coerceIn(0f, 1f),
                                        newScale
                                    )
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

private fun mapFontFamily(font: DecorationFont): FontFamily = when (font) {
    DecorationFont.Sans -> FontFamily.SansSerif
    DecorationFont.Serif -> FontFamily.Serif
    DecorationFont.Mono -> FontFamily.Monospace
    DecorationFont.Cursive -> FontFamily.Cursive
    DecorationFont.Display -> FontFamily.Serif
    DecorationFont.Rounded -> FontFamily.SansSerif
    DecorationFont.Condensed -> FontFamily.SansSerif
}

private fun mapFontWeight(weight: DecorationFontWeight): FontWeight = when (weight) {
    DecorationFontWeight.Light -> FontWeight.Light
    DecorationFontWeight.Regular -> FontWeight.Normal
    DecorationFontWeight.Medium -> FontWeight.Medium
    DecorationFontWeight.SemiBold -> FontWeight.SemiBold
    DecorationFontWeight.Bold -> FontWeight.Bold
}
