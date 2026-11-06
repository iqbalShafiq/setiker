package presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.theme.NeubrutalPressedShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalFill
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadowColor
import presentation.theme.NeubrutalBorderRole
import presentation.theme.neubrutalBorderFor
import presentation.theme.neubrutalDisabledFill

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
        AppIcon(
            glyph = icon,
            contentDescription = contentDescription,
            modifier = iconModifier,
            tint = iconTint,
            size = iconSize,
            enabled = enabled
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
        AppIcon(
            glyph = icon,
            contentDescription = contentDescription,
            modifier = iconModifier,
            tint = tint,
            size = iconSize,
            enabled = enabled
        )
    }
}

/**
 * The shared bordered surface. It keeps the press scale and the shadow shrink in one place so
 * every icon button in a bar behaves the same way.
 *
 * [interaction] is the only thing that differs between a plain action and a toggle, so it is the
 * one thing a caller supplies: the shell, the shadow, the gloss and the press spring stay identical
 * no matter whether the control reports a click or a boolean state.
 */
@Composable
internal fun NeubrutalSurface(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    containerColor: Color = neubrutalCardSurface(),
    pressedScale: Float = SetikerMotion.PRESS_SCALE_SMALL,
    label: String = "surface_icon_scale",
    interaction: (PressFeedback) -> Modifier,
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
                color = if (enabled) {
                    neubrutalShadowColor()
                } else {
                    neubrutalShadowColor().copy(alpha = 0.4f)
                }
            )
            .clip(shape)
            .neubrutalFill(
                color = if (enabled) containerColor else neubrutalDisabledFill(containerColor),
                cornerRadius = NeubrutalSmallRadius
            )
            .neubrutalBorderWithGloss(
                color = neubrutalBorderFor(
                    if (enabled) NeubrutalBorderRole.Container else NeubrutalBorderRole.Disabled
                ),
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor(
                    onFilledSurface = containerColor != neubrutalCardSurface()
                )
            )
            .then(interaction(pressFeedback)),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** A bordered surface that acts as a button. */
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
    NeubrutalSurface(
        modifier = modifier,
        size = size,
        enabled = enabled,
        containerColor = containerColor,
        pressedScale = pressedScale,
        label = label,
        interaction = { feedback ->
            Modifier.clickableNoRipple(
                interactionSource = feedback.interactionSource,
                enabled = enabled,
                onClick = onClick
            )
        },
        content = content
    )
}

/**
 * A bordered surface that acts as a switch.
 *
 * The state comes from `toggleable`, so a reader hears "checked / not checked" and a test can ask
 * for it directly. Nothing here writes a `stateDescription` by hand — the point of the toggle is
 * that it cannot drift out of sync with the value it renders.
 */
@Composable
internal fun NeubrutalSurfaceToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    role: Role = Role.Checkbox,
    containerColor: Color = neubrutalCardSurface(),
    pressedScale: Float = SetikerMotion.PRESS_SCALE_SMALL,
    label: String = "surface_toggle_scale",
    content: @Composable BoxScope.() -> Unit
) {
    NeubrutalSurface(
        modifier = modifier,
        size = size,
        enabled = enabled,
        containerColor = containerColor,
        pressedScale = pressedScale,
        label = label,
        interaction = { feedback ->
            Modifier.toggleableNoRipple(
                interactionSource = feedback.interactionSource,
                value = checked,
                enabled = enabled,
                role = role,
                onValueChange = onCheckedChange
            )
        },
        content = content
    )
}

