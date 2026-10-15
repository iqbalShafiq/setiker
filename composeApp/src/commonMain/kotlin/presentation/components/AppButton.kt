package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalButtonRadius
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
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
    val pressFeedback = rememberPressFeedback(
        pressedScale = 0.97f,
        enabled = enabled,
        label = "primary_button_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shadowX = if (pressFeedback.isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shadowY = if (pressFeedback.isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shape = RoundedCornerShape(NeubrutalButtonRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressFeedback)
            .neubrutalShadow(
                offsetX = shadowX,
                offsetY = shadowY,
                cornerRadius = NeubrutalButtonRadius,
                color = shadow
            )
            .clip(shape)
            .background(
                color = if (enabled) AccentCoral else AccentCoral.copy(alpha = 0.4f),
            )
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalButtonRadius,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
            )
            .clickable(
                interactionSource = pressFeedback.interactionSource,
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
    val pressFeedback = rememberPressFeedback(
        pressedScale = 0.97f,
        enabled = enabled,
        label = "secondary_button_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val surface = neubrutalCardSurface()
    val onSurface = neubrutalOnSurface()
    val shadowX = if (pressFeedback.isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shadowY = if (pressFeedback.isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shape = RoundedCornerShape(NeubrutalButtonRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressFeedback)
            .neubrutalShadow(
                offsetX = shadowX,
                offsetY = shadowY,
                cornerRadius = NeubrutalButtonRadius,
                color = shadow
            )
            .clip(shape)
            .background(
                color = if (enabled) surface else surface.copy(alpha = 0.5f),
            )
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalButtonRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .clickable(
                interactionSource = pressFeedback.interactionSource,
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
