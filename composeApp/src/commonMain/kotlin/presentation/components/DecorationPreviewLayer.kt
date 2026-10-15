package presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.resolveEmojiStyle
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import presentation.theme.AccentCoral
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.remove_decoration
import setiker.composeapp.generated.resources.resize_decoration

@Composable
fun DecorationPreviewLayer(
    decorations: List<StickerDecoration>,
    selectedDecorationId: String?,
    onSelectDecoration: (String?) -> Unit,
    onUpdateDecoration: (id: String, centerX: Float, centerY: Float, scale: Float) -> Unit,
    onDeleteDecoration: (String) -> Unit,
    /**
     * Called once when a move/resize gesture starts, before any transform is applied. Callers that
     * keep undo history use it to snapshot the pre-gesture state.
     */
    onDecorationTransformStart: (String) -> Unit = {},
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
            // The live position is kept here and read only by the placement lambda below: a drag then
            // re-places this one item instead of pushing a new state object through the editor (and
            // its undo history) on every pointer event. The final value is committed on drag end.
            var renderCenter by remember(decoration.id) {
                mutableStateOf(Offset(decoration.centerX, decoration.centerY))
            }
            LaunchedEffect(decoration.centerX, decoration.centerY) {
                renderCenter = Offset(decoration.centerX, decoration.centerY)
            }

            Box(
                modifier = Modifier
                    .offset {
                        val center = renderCenter
                        IntOffset(
                            (center.x.coerceIn(0f, 1f) * widthPx - itemWidth / 2f).roundToInt(),
                            (center.y.coerceIn(0f, 1f) * heightPx - itemHeight / 2f).roundToInt()
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
                                onDecorationTransformStart(decoration.id)
                            },
                            onDragEnd = {
                                // One commit per gesture: undo history gets a single entry and the
                                // editor recomposes once instead of on every pointer event.
                                onUpdateDecoration(
                                    decoration.id,
                                    startCenterX.coerceIn(0f, 1f),
                                    startCenterY.coerceIn(0f, 1f),
                                    fixedScale.coerceIn(
                                        DecorationRenderSpec.MIN_SCALE,
                                        DecorationRenderSpec.MAX_SCALE
                                    )
                                )
                            },
                            onDragCancel = {
                                // Aborted gesture: drop the local preview, keep the state untouched.
                                renderCenter = Offset(latestCenterX, latestCenterY)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            startCenterX = (startCenterX + dragAmount.x / widthPx).coerceIn(0f, 1f)
                            startCenterY = (startCenterY + dragAmount.y / heightPx).coerceIn(0f, 1f)
                            renderCenter = Offset(startCenterX, startCenterY)
                        }
                    }
                    .pointerInput(decoration.id) {
                        detectTapGestures(onTap = { onSelectDecoration(decoration.id) })
                    }
                    .then(
                        Modifier.border(
                            width = NeubrutalBorderWidth,
                            // Accent rather than plain black/white: the outline sits on top of arbitrary
                            // artwork, where a theme-dependent stroke is invisible on one of the two.
                            color = if (selectedDecorationId == decoration.id) AccentCoral else Color.Transparent,
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
                    // Both affordances sit on top of arbitrary artwork and were previously invisible to
                    // accessibility services: the badge only had a tap detector and the handle only a
                    // drag detector, so neither was announced or reachable.
                    val removeLabel = stringResource(Res.string.remove_decoration)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(ErrorRed)
                            .neubrutalBorderWithGloss(
                                color = neubrutalBorderColor(),
                                cornerRadius = 9.dp,
                                shape = CircleShape,
                                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
                            )
                            .semantics { contentDescription = removeLabel }
                            .clickable(onClick = { onDeleteDecoration(decoration.id) })
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    val handleSize = 20.dp
                    val resizeLabel = stringResource(Res.string.resize_decoration)
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
                            .semantics { contentDescription = resizeLabel }
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
                                        onDecorationTransformStart(decoration.id)
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
