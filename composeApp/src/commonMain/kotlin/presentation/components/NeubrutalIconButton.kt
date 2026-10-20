package presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface

/**
 * A bare icon button that speaks the app's press language.
 *
 * A raw Material `IconButton` keeps the Material ripple, which means the app ends up with two
 * different press languages: a ripple here and the neubrutal scale + shadow everywhere else.
 * This button has no ripple, scales and shrinks its shadow on press like every other control, and
 * guarantees a 48 dp touch target even when the glyph is smaller.
 */
@Composable
fun NeubrutalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color = neubrutalOnSurface(),
    pressedScale: Float = 0.92f,
    label: String = "icon_button_scale"
) {
    val pressFeedback = rememberPressFeedback(
        pressedScale = pressedScale,
        enabled = enabled,
        label = label
    )

    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressFeedback)
            .clickableNoRipple(
                interactionSource = pressFeedback.interactionSource,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (enabled) tint else neubrutalMutedOnSurface()
        )
    }
}
