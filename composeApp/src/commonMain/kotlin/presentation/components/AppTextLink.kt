package presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import presentation.theme.AccentCoral
import presentation.theme.SetikerMotion

/**
 * A text action in the app's language: no ripple, a small press scale and the same haptic as a
 * selection. Material's `TextButton` is a different object with a different feel; this is the one
 * used for "Forgot password?", "Skip", "Mark all read" and the inline dialog actions.
 */
@Composable
fun AppTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AccentCoral,
    textAlign: TextAlign = TextAlign.Center
) {
    val haptics = rememberAppHaptics()
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_SUBTLE,
        enabled = enabled,
        label = "text_link_scale"
    )

    Text(
        text = text,
        modifier = modifier
            .padding(vertical = 8.dp)
            .pressScale(pressFeedback)
            .clickableNoRipple(
                interactionSource = pressFeedback.interactionSource,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.selection()
                    onClick()
                }
            ),
        color = if (enabled) color else color.copy(alpha = 0.45f),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
        fontWeight = FontWeight.SemiBold,
        textAlign = textAlign
    )
}
