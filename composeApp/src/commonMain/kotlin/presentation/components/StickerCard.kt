package presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import org.jetbrains.compose.resources.stringResource
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalPressedShadowOffset
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.delete_sticker
import setiker.composeapp.generated.resources.sticker_fallback

@Composable
fun StickerCard(
    sticker: Sticker,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showDecorations: Boolean = false,
    fallbackContentDescription: String? = null,
    lifted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val cardShape = RoundedCornerShape(NeubrutalCardRadius)
    val innerShape = RoundedCornerShape(NeubrutalSmallRadius)
    val haptics = rememberAppHaptics()
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_REGULAR,
        label = "sticker_card_scale"
    )
    val deleteFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_TINY,
        label = "sticker_card_delete_scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = if (lifted) 8.dp else NeubrutalShadowOffset,
                pressedOffset = if (lifted) 4.dp else NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = neubrutalShadowColor()
            )
            .clip(cardShape)
            .neubrutalFill(color = neubrutalCardSurface(), cornerRadius = NeubrutalCardRadius)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickableNoRipple(
                        interactionSource = pressFeedback.interactionSource,
                        onClick = onClick,
                        onLongClick = {
                            haptics.longPress()
                            onLongClick()
                        }
                    )
                } else {
                    Modifier.clickableNoRipple(
                        interactionSource = pressFeedback.interactionSource,
                        onClick = onClick
                    )
                }
            )
            .padding(4.dp)
    ) {
        NeubrutalAsyncImage(
            model = sticker.imageFile,
            contentDescription = sticker.accessibilityText
                ?: fallbackContentDescription
                ?: stringResource(Res.string.sticker_fallback),
            modifier = Modifier
                .fillMaxSize()
                .clip(innerShape),
            contentScale = ContentScale.Fit
        )

        // imageFile already includes baked decorations when sourceImageFile differs (saved stickers).
        val decorationsAlreadyBaked = sticker.decorations.isNotEmpty() &&
            sticker.sourceImageFile != null &&
            sticker.imageFile != sticker.sourceImageFile

        if (showDecorations && sticker.decorations.isNotEmpty() && !decorationsAlreadyBaked) {
            ReadOnlyDecorationOverlay(
                decorations = sticker.decorations,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(innerShape)
            )
        }

        if (onDeleteClick != null) {
            val deleteShape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(28.dp)
                    .pressScale(deleteFeedback)
                    .pressShadow(
                        feedback = deleteFeedback,
                        restOffset = NeubrutalSmallShadowOffset,
                        pressedOffset = NeubrutalPressedShadowOffset,
                        cornerRadius = 6.dp,
                        color = neubrutalShadowColor()
                    )
                    .clip(deleteShape)
                    .neubrutalFill(color = ErrorRed, cornerRadius = 6.dp)
                    .neubrutalBorderWithGloss(
                        color = border,
                        width = NeubrutalSmallShadowOffset,
                        cornerRadius = 6.dp,
                        highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
                    )
                    .clickableNoRipple(
                        interactionSource = deleteFeedback.interactionSource,
                        onClick = {
                            haptics.selection()
                            onDeleteClick()
                        }
                    ),
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
