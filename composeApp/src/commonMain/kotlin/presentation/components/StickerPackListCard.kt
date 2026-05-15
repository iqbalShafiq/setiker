package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import domain.model.StickerPack
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.stickers_with_count

@Composable
fun StickerPackListCard(
    pack: StickerPack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = 4.dp,
                offsetY = 4.dp,
                cornerRadius = 16.dp,
                color = shadow
            )
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(
                width = 2.dp,
                color = border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tray image
        AsyncImage(
            model = pack.trayImageFile,
            contentDescription = pack.name,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surface)
                .border(
                    width = 2.dp,
                    color = border,
                    shape = RoundedCornerShape(12.dp)
                )
                .neubrutalShadow(
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    cornerRadius = 12.dp,
                    color = shadow
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = pack.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = neubrutalOnSurface(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(Res.string.stickers_with_count, pack.stickers.size),
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = neubrutalMutedOnSurface()
        )
    }
}

// MARK: - Preview

@Preview
@Composable
private fun StickerPackListCardPreview() {
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
        StickerPackListCard(pack = mockPack, onClick = {})
    }
}
