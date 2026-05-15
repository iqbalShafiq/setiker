package presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalButtonRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "danger_button_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shadowX = if (isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shadowY = if (isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shape = RoundedCornerShape(NeubrutalButtonRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .neubrutalShadow(
                offsetX = shadowX,
                offsetY = shadowY,
                cornerRadius = NeubrutalButtonRadius,
                color = shadow
            )
            .clip(shape)
            .background(
                color = if (enabled) ErrorRed else ErrorRed.copy(alpha = 0.4f),
            )
            .border(
                width = NeubrutalBorderWidth,
                color = border,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NeubrutalWhite
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = NeubrutalWhite
            )
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun AppDangerButtonPreview() {
    MaterialTheme {
        AppDangerButton(text = "Log Out", onClick = {})
    }
}
