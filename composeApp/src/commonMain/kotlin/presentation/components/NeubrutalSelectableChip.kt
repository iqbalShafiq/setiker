package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@Composable
fun NeubrutalSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pressFeedback = rememberPressFeedback(
        pressedScale = 0.97f,
        label = "neubrutal_selectable_chip_scale"
    )
    val shadowOffset = if (pressFeedback.isPressed) NeubrutalSmallShadowOffset else NeubrutalShadowOffset

    Box(
        modifier = modifier
            .pressScale(pressFeedback)
            .neubrutalShadow(
                offsetX = shadowOffset,
                offsetY = shadowOffset,
                cornerRadius = 99.dp,
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .background(
                if (selected) {
                    AccentCoral
                } else {
                    neubrutalCardSurface()
                }
            )
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = 99.dp,
                shape = CircleShape,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = selected)
            )
            .clickable(
                interactionSource = pressFeedback.interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                neubrutalMutedOnSurface().copy(alpha = 0.9f)
            }
        )
    }
}
