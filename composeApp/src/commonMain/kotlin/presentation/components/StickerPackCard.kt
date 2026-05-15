package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import domain.model.StickerPack
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@Composable
fun StickerPackCard(
    pack: StickerPack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    val innerShape = RoundedCornerShape(NeubrutalSmallRadius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = shadow
            )
            .clip(shape)
            .background(surface)
            .border(
                width = NeubrutalBorderWidth,
                color = border,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        AsyncImage(
            model = pack.trayImageFile,
            contentDescription = pack.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(innerShape)
                .background(surface)
                .border(
                    width = NeubrutalBorderWidth,
                    color = border,
                    shape = innerShape
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = pack.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${pack.stickers.size} stickers",
            style = MaterialTheme.typography.bodyMedium,
            color = neubrutalMutedOnSurface()
        )
    }
}

// MARK: - Preview

@Preview
@Composable
private fun StickerPackCardPreview() {
    val mockPack = StickerPack(
        identifier = "pack_preview",
        name = "Funny Cats",
        publisher = "CatLover",
        trayImageFile = "",
        stickers = listOf(
            domain.model.Sticker(imageFile = ""),
            domain.model.Sticker(imageFile = ""),
            domain.model.Sticker(imageFile = "")
        )
    )
    MaterialTheme {
        StickerPackCard(pack = mockPack, onClick = {})
    }
}
