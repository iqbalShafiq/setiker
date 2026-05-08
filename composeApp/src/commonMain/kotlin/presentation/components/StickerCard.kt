package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import domain.model.Sticker
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.delete_sticker
import setiker.composeapp.generated.resources.sticker_fallback

@Composable
fun StickerCard(
    sticker: Sticker,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .neubrutalShadow(
                offsetX = 3.dp,
                offsetY = 3.dp,
                cornerRadius = 12.dp,
                color = NeubrutalBlack
            )
            .clip(RoundedCornerShape(12.dp))
            .background(NeubrutalWhite)
            .border(
                width = 2.dp,
                color = NeubrutalBlack,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = sticker.imageFile,
            contentDescription = sticker.accessibilityText ?: stringResource(Res.string.sticker_fallback),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        if (onDeleteClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorRed)
                    .border(
                        width = 1.5.dp,
                        color = NeubrutalBlack,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete_sticker),
                    modifier = Modifier.size(16.dp),
                    tint = NeubrutalWhite
                )
            }
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun StickerCardPreview() {
    val mockSticker = Sticker(
        imageFile = "",
        emojis = listOf("😂", "🐱"),
        accessibilityText = "Laughing cat"
    )
    MaterialTheme {
        StickerCard(
            sticker = mockSticker,
            onClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview
@Composable
private fun StickerCardWithoutDeletePreview() {
    val mockSticker = Sticker(imageFile = "", emojis = listOf("😂"))
    MaterialTheme {
        StickerCard(sticker = mockSticker, onClick = {})
    }
}
