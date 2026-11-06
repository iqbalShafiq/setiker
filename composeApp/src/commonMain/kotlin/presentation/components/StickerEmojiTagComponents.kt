package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
import presentation.theme.neubrutalFill
import presentation.theme.NeubrutalBorderRole
import presentation.theme.neubrutalBorderFor
import presentation.theme.neubrutalDisabledFill
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.remove_emoji

/**
 * The height of a tag chip and of the add pill.
 *
 * The two sit next to each other in a row, so they have to be the same height, and they have to
 * be the same height as every other chip in the app. The remove button inside a chip used to be
 * the full 48 dp touch target, which made the chip twice as tall as the pill next to it.
 */
private val TagChipHeight = 32.dp

/** The touch target of the remove button inside a chip. */
private val TagRemoveTarget = 24.dp

@Composable
fun StickerEmojiTagChip(
    emoji: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(TagChipHeight)
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = 50.dp, // pill
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .neubrutalFill(color = neubrutalCardSurface(), cornerRadius = 99.dp)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = 50.dp,
                shape = CircleShape,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyMedium,
            color = neubrutalOnSurface()
        )
        NeubrutalBareIconButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(Res.string.remove_emoji),
            onClick = onRemove,
            size = TagRemoveTarget,
            iconSize = 13.dp,
            tint = neubrutalSubtleOnSurface(),
            pressedScale = SetikerMotion.PRESS_SCALE_TINY,
            label = "emoji_remove_scale"
        )
    }
}

@Composable
fun NeubrutalAddTagPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = rememberAppHaptics()
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_REGULAR,
        enabled = enabled,
        label = "add_tag_pill_scale"
    )

    Box(
        modifier = modifier
            .height(TagChipHeight)
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalShadowOffset,
                pressedOffset = NeubrutalSmallShadowOffset,
                cornerRadius = 50.dp,
                color = if (enabled) {
                    neubrutalShadowColor()
                } else {
                    neubrutalShadowColor().copy(alpha = 0.4f)
                }
            )
            .clip(CircleShape)
            .neubrutalFill(
                color = if (enabled) neubrutalCardSurface() else neubrutalDisabledFill(neubrutalCardSurface()),
                cornerRadius = 99.dp
            )
            .neubrutalBorderWithGloss(
                color = neubrutalBorderFor(
                    if (enabled) NeubrutalBorderRole.Container else NeubrutalBorderRole.Disabled
                ),
                cornerRadius = 50.dp,
                shape = CircleShape,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .clickableNoRipple(
                interactionSource = pressFeedback.interactionSource,
                enabled = enabled,
                onClick = {
                    haptics.selection()
                    onClick()
                }
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) AccentCoral else AccentCoral.copy(alpha = 0.4f),
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
            emoji = "dY",
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

