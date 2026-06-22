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
import androidx.compose.runtime.key
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
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.resolveEmojiStyle
import kotlin.math.roundToInt
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor

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
            key(decoration.id) {
            val latestCenterX by rememberUpdatedState(decoration.centerX)
            val latestCenterY by rememberUpdatedState(decoration.centerY)
            val latestScale by rememberUpdatedState(decoration.scale)
            val scale = decoration.scale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)
            val itemSizePx = when (decoration) {
                is TextDecoration -> minDim * DecorationRenderSpec.TEXT_BOX_RATIO * scale
                is EmojiDecoration -> minDim * DecorationRenderSpec.EMOJI_BOX_RATIO * scale
                is ImageDecoration -> minDim * DecorationRenderSpec.IMAGE_BASE_RATIO * scale
            }

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
            val itemWidth = when (decoration) {
                is TextDecoration -> textMetrics!!.widthPx
                else -> itemSizePx
            }
            val itemHeight = when (decoration) {
                is TextDecoration -> textMetrics!!.heightPx
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
                            width = NeubrutalBorderWidth,
                            color = if (selectedDecorationId == decoration.id) Color.Black else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                    )
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

                if (selectedDecorationId == decoration.id) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .neubrutalBorderWithGloss(
                                color = Color.Black,
                                cornerRadius = 9.dp,
                                shape = CircleShape,
                                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
                            )
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
                            .neubrutalBorderWithGloss(
                                color = Color.Black,
                                cornerRadius = 10.dp,
                                shape = CircleShape,
                                highlightColor = neubrutalGlossyHighlightColor()
                            )
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
}

// `mapFontFamily` and `mapFontWeight` now live in `DecorationBottomSheets.kt` as
// `internal` helpers so both static and animated editors share identical decoration
// rendering and picker chips without duplicating the mapping logic.

// MARK: - Previews

@Preview
@Composable
private fun DecorationPreviewLayerPreview() {
    val mockDecorations = listOf(
        TextDecoration(
            id = "txt_1",
            text = "Hello World",
            font = domain.model.DecorationFont.Fredoka,
            fontWeight = domain.model.DecorationFontWeight.Bold,
            style = domain.model.TextDecorationStyle.ClassicOutline,
            textColorArgb = 0xFF000000L,
            centerX = 0.5f,
            centerY = 0.35f,
            scale = 1f
        ),
        EmojiDecoration(
            id = "emoji_1",
            emoji = "🔥",
            centerX = 0.5f,
            centerY = 0.65f,
            scale = 1.5f
        )
    )
    MaterialTheme {
        DecorationPreviewLayer(
            decorations = mockDecorations,
            selectedDecorationId = "txt_1",
            onSelectDecoration = {},
            onUpdateDecoration = { _, _, _, _ -> },
            onDeleteDecoration = {}
        )
    }
}
