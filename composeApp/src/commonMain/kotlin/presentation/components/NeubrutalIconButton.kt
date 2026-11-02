package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.NeubrutalPressedShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadowColor

/**
 * The surface icon button used in top and bottom bars: a bordered square with a hard shadow.
 *
 * This is the only implementation of the bordered icon button. The bare variant lives below it
 * and shares the same press spring, so the two no longer disagree about how a button reacts.
 */
@Composable
fun NeubrutalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    iconTint: Color = neubrutalOnSurface(),
    pressedScale: Float = SetikerMotion.PRESS_SCALE_SMALL,
    label: String = "icon_button_scale",
    iconModifier: Modifier = Modifier
) {
    NeubrutalSurfaceIcon(
        modifier = modifier,
        size = size,
        enabled = enabled,
        pressedScale = pressedScale,
        label = label,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize).then(iconModifier),
            tint = if (enabled) iconTint else neubrutalMutedOnSurface()
        )
    }
}

/**
 * A bare icon button that speaks the app's press language.
 *
 * A raw Material `IconButton` keeps the Material ripple, which means the app ends up with two
 * different press languages: a ripple here and the neubrutal scale everywhere else. This button
 * has no ripple, scales on press like every other control, and guarantees a 48 dp touch target
 * even when the glyph is smaller.
 */
@Composable
fun NeubrutalBareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color = neubrutalOnSurface(),
    pressedScale: Float = SetikerMotion.PRESS_SCALE_SMALL,
    label: String = "bare_icon_button_scale",
    iconModifier: Modifier = Modifier
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
            modifier = Modifier.size(iconSize).then(iconModifier),
            tint = if (enabled) tint else neubrutalMutedOnSurface()
        )
    }
}

/**
 * The shared bordered surface. It keeps the press scale and the shadow shrink in one place so
 * every icon button in a bar behaves the same way.
 */
@Composable
internal fun NeubrutalSurfaceIcon(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    containerColor: Color = neubrutalCardSurface(),
    pressedScale: Float = SetikerMotion.PRESS_SCALE_SMALL,
    label: String = "surface_icon_scale",
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val pressFeedback = rememberPressFeedback(
        pressedScale = pressedScale,
        enabled = enabled,
        label = label
    )
    val shape = RoundedCornerShape(NeubrutalSmallRadius)

    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalSmallShadowOffset,
                pressedOffset = NeubrutalPressedShadowOffset,
                cornerRadius = NeubrutalSmallRadius,
                color = neubrutalShadowColor()
            )
            .clip(shape)
            .background(containerColor)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor(
                    onFilledSurface = containerColor != neubrutalCardSurface()
                )
            )
            .clickableNoRipple(
                interactionSource = pressFeedback.interactionSource,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
