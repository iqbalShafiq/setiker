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
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow

@Composable
fun StickerPackCard(
    pack: StickerPack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = 4.dp,
                offsetY = 4.dp,
                cornerRadius = 20.dp,
                color = NeubrutalBlack
            )
            .clip(RoundedCornerShape(20.dp))
            .background(NeubrutalWhite)
            .border(
                width = 2.dp,
                color = NeubrutalBlack,
                shape = RoundedCornerShape(20.dp)
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
                .clip(RoundedCornerShape(12.dp))
                .background(NeubrutalWhite)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = pack.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = NeubrutalBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${pack.stickers.size} stickers",
            style = MaterialTheme.typography.bodyMedium,
            color = NeubrutalGray
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
