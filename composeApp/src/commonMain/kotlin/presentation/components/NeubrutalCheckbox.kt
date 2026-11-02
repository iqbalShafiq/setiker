package presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/**
 * The app's checkbox: a hard-shadow square that fills with coral and pops the tick in.
 *
 * This is a visual only. The row around it owns the toggle so the label is part of the hit target.
 */
@Composable
fun NeubrutalCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_FAST_MS),
        label = "checkbox_fill"
    )
    val shape = RoundedCornerShape(NeubrutalSmallRadius)

    Box(
        modifier = modifier
            .size(26.dp)
            .neubrutalShadow(
                offsetX = NeubrutalSmallShadowOffset,
                offsetY = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalSmallRadius,
                color = if (enabled) neubrutalShadowColor() else neubrutalShadowColor().copy(alpha = 0.4f)
            )
            .clip(shape)
            .background(lerp(neubrutalCardSurface(), AccentCoral, fill))
            .neubrutalBorderWithGloss(
                color = if (enabled) neubrutalBorderColor() else neubrutalBorderColor().copy(alpha = 0.4f),
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = checked)
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn(SetikerMotion.out(SetikerMotion.DURATION_FAST_MS)) +
                scaleIn(
                    initialScale = 0.6f,
                    animationSpec = SetikerMotion.PressSpring
                ),
            exit = fadeOut(SetikerMotion.out(SetikerMotion.DURATION_FAST_MS)) +
                scaleOut(targetScale = 0.6f)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
    }
}
