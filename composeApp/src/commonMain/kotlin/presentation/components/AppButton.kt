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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@Composable
fun AppPrimaryButton(
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
        label = "primary_button_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .neubrutalShadow(
                offsetX = if (isPressed && enabled) 1.dp else 4.dp,
                offsetY = if (isPressed && enabled) 1.dp else 4.dp,
                cornerRadius = 16.dp,
                color = shadow
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = if (enabled) AccentCoral else AccentCoral.copy(alpha = 0.4f),
            )
            .border(
                width = 2.dp,
                color = border,
                shape = RoundedCornerShape(16.dp)
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
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = NeubrutalWhite
        )
    }
}

@Composable
fun AppSecondaryButton(
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
        label = "secondary_button_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val surface = neubrutalCardSurface()
    val onSurface = neubrutalOnSurface()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .neubrutalShadow(
                offsetX = if (isPressed && enabled) 1.dp else 4.dp,
                offsetY = if (isPressed && enabled) 1.dp else 4.dp,
                cornerRadius = 16.dp,
                color = shadow
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = if (enabled) surface else surface.copy(alpha = 0.5f),
            )
            .border(
                width = 2.dp,
                color = border,
                shape = RoundedCornerShape(16.dp)
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
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) onSurface else onSurface.copy(alpha = 0.4f)
        )
    }
}

// MARK: - Previews

@Preview
@Composable
private fun AppPrimaryButtonPreview() {
    MaterialTheme {
        AppPrimaryButton(text = "Save Pack", onClick = {})
    }
}

@Preview
@Composable
private fun AppSecondaryButtonPreview() {
    MaterialTheme {
        AppSecondaryButton(text = "Cancel", onClick = {})
    }
}

@Preview
@Composable
private fun AppButtonsCombinedPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AppPrimaryButton(text = "Primary Button", onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            AppSecondaryButton(text = "Secondary Button", onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            AppPrimaryButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
