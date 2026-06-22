package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import presentation.createpack.DraftSticker
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlight
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.split_sticker_index

@Composable
fun ApiActionSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val border = neubrutalBorderColor()
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = neubrutalShadowColor()
            )
            .clip(shape)
            .background(neubrutalCardSurface())
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface()
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .neubrutalShadow(
                                offsetX = NeubrutalSmallShadowOffset,
                                offsetY = NeubrutalSmallShadowOffset,
                                cornerRadius = NeubrutalSmallRadius,
                                color = shadow
                            )
                            .clip(innerShape)
                            .background(surface)
                            .border(
                                width = if (selectedIndices.contains(index)) 3.dp else NeubrutalBorderWidth,
                                color = if (selectedIndices.contains(index)) AccentCoral else border,
                                shape = innerShape
                            )
                            .neubrutalGlossyHighlight(
                                cornerRadius = NeubrutalSmallRadius,
                                highlightColor = neubrutalGlossyHighlightColor()
                            )
                            .clickable { onToggle(index) }
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

// MARK: - Previews

@Preview
@Composable
private fun ApiActionSectionPreview() {
    MaterialTheme {
        ApiActionSection(
            title = "API Actions",
            content = {
                Text(
                    text = "Sample content",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
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
