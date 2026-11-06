package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.resources.stringResource
import presentation.icons.SetikerIcons
import presentation.theme.AppIconSize
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill
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
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    val thumbShape = RoundedCornerShape(NeubrutalSmallRadius)
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_SUBTLE,
        label = "sticker_pack_card_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalShadowOffset,
                pressedOffset = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = shadow
            )
            .clip(shape)
            .neubrutalFill(color = surface, cornerRadius = NeubrutalCardRadius)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .clickableNoRipple(
                interactionSource = pressFeedback.interactionSource,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tray image
        NeubrutalAsyncImage(
            model = pack.trayImageFile,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .neubrutalShadow(
                    offsetX = NeubrutalSmallShadowOffset,
                    offsetY = NeubrutalSmallShadowOffset,
                    cornerRadius = NeubrutalSmallRadius,
                    color = shadow
                )
                .clip(thumbShape)
                .neubrutalFill(color = surface, cornerRadius = NeubrutalSmallRadius)
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = NeubrutalSmallRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
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
        AppIcon(
            glyph = SetikerIcons.Chevron,
            contentDescription = null,
            tint = neubrutalMutedOnSurface(),
            size = AppIconSize.Compact
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
            Sticker(imageFile = ""),
            Sticker(imageFile = ""),
            Sticker(imageFile = "")
        )
    )
    MaterialTheme {
        StickerPackListCard(pack = mockPack, onClick = {})
    }
}
