package presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import presentation.createpack.DraftSticker
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalPressedShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlight
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill
import presentation.theme.NeubrutalBorderRole
import presentation.theme.neubrutalBorderFor
import presentation.theme.neubrutalBorderWidthFor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.split_sticker_index

@Composable
fun SelectableStickerGrid(
    stickers: List<DraftSticker>,
    selectedIndices: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stickers.chunked(4).forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val border = neubrutalBorderColor()
                val shadow = neubrutalShadowColor()
                val surface = neubrutalCardSurface()
                val innerShape = RoundedCornerShape(NeubrutalSmallRadius)
                rowItems.forEachIndexed { itemIndex, draft ->
                    val index = (rowIndex * 4) + itemIndex
                    val selected = selectedIndices.contains(index)
                    val haptics = rememberAppHaptics()
                    val pressFeedback = rememberPressFeedback(
                        pressedScale = SetikerMotion.PRESS_SCALE_REGULAR,
                        label = "selectable_sticker_scale"
                    )
                    val borderWidth by animateDpAsState(
                        targetValue = neubrutalBorderWidthFor(
                            if (selected) NeubrutalBorderRole.Selected else NeubrutalBorderRole.Container
                        ),
                        animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_FAST_MS),
                        label = "selectable_sticker_border_width"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = neubrutalBorderFor(
                            if (selected) NeubrutalBorderRole.Selected else NeubrutalBorderRole.Container
                        ),
                        animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_FAST_MS),
                        label = "selectable_sticker_border_color"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .pressScale(pressFeedback)
                            .pressShadow(
                                feedback = pressFeedback,
                                restOffset = NeubrutalSmallShadowOffset,
                                pressedOffset = NeubrutalPressedShadowOffset,
                                cornerRadius = NeubrutalSmallRadius,
                                color = shadow
                            )
                            .clip(innerShape)
                            .neubrutalFill(color = surface, cornerRadius = NeubrutalSmallRadius)
                            .border(
                                width = borderWidth,
                                color = borderColor,
                                shape = innerShape
                            )
                            .neubrutalGlossyHighlight(
                                cornerRadius = NeubrutalSmallRadius,
                                highlightColor = neubrutalGlossyHighlightColor()
                            )
                            .toggleable(
                                value = selected,
                                interactionSource = pressFeedback.interactionSource,
                                indication = null,
                                role = Role.Checkbox,
                                onValueChange = {
                                    haptics.selection()
                                    onToggle(index)
                                }
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        AsyncImage(
                            model = draft.imagePath,
                            contentDescription = stringResource(Res.string.split_sticker_index, index),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        ReadOnlyDecorationOverlay(
                            decorations = draft.decorations,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                repeat(4 - rowItems.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SelectableStickerGridPreview() {
    MaterialTheme {
        SelectableStickerGrid(
            stickers = listOf(
                DraftSticker(imagePath = ""),
                DraftSticker(imagePath = ""),
                DraftSticker(imagePath = "")
            ),
            selectedIndices = setOf(0),
            onToggle = {}
        )
    }
}

