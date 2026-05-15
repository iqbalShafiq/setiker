package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.remove_emoji

@Composable
fun StickerEmojiTagChip(
    emoji: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = 50.dp, // pill
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .background(neubrutalCardSurface())
            .border(
                width = NeubrutalBorderWidth,
                color = neubrutalBorderColor(),
                shape = CircleShape
            )
            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyLarge,
            color = neubrutalOnSurface()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(22.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.remove_emoji),
                modifier = Modifier.size(14.dp),
                tint = neubrutalSubtleOnSurface()
            )
        }
    }
}

@Composable
fun NeubrutalAddTagPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = 50.dp, // pill
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .background(neubrutalCardSurface())
            .border(
                width = NeubrutalBorderWidth,
                color = neubrutalBorderColor(),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AccentCoral,
            fontWeight = FontWeight.Medium
        )
    }
}

// MARK: - Previews
@Preview
@Composable
private fun StickerEmojiTagChipPreview() {
    MaterialTheme {
        StickerEmojiTagChip(
            emoji = "🔥",
            onRemove = {}
        )
    }
}

@Preview
@Composable
private fun NeubrutalAddTagPillPreview() {
    MaterialTheme {
        NeubrutalAddTagPill(
            label = "+ Add Tag",
            onClick = {}
        )
    }
}
