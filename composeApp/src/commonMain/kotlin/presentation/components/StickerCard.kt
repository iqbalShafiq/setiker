package presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import domain.model.Sticker

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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = sticker.imageFile,
            contentDescription = sticker.accessibilityText ?: "Sticker",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        if (onDeleteClick != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            ) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete sticker",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}
