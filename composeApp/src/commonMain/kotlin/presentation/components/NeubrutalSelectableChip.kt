package presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

@Composable
fun NeubrutalSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeubrutalChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = chipContentColor(selected)
        )
    }
}

/**
 * The selectable chip with a caller-supplied label.
 *
 * Use it when the label needs its own typography, such as the font previews in the text
 * decoration sheet. The surface, press feedback and selection animation stay identical.
 */
@Composable
fun NeubrutalFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    NeubrutalChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        content = content
    )
}

@Composable
internal fun NeubrutalChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptics = rememberAppHaptics()
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_REGULAR,
        label = "neubrutal_chip_scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) AccentCoral else neubrutalCardSurface(),
        animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_FAST_MS),
        label = "chip_background"
    )

    Box(
        modifier = modifier
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalShadowOffset,
                pressedOffset = NeubrutalSmallShadowOffset,
                cornerRadius = 99.dp,
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .neubrutalFill(color = backgroundColor, cornerRadius = 99.dp)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = 99.dp,
                shape = CircleShape,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = selected)
            )
            .selectable(
                selected = selected,
                interactionSource = pressFeedback.interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = {
                    haptics.selection()
                    onClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun chipContentColor(selected: Boolean) = if (selected) {
    MaterialTheme.colorScheme.onPrimary
} else {
    neubrutalMutedOnSurface().copy(alpha = 0.9f)
}
