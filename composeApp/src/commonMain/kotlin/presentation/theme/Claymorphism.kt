package presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a hard offset shadow (neubrutalism style).
 * Shadow drops to the bottom-right with a solid dark color,
 * and follows the rounded corners of the content.
 */
fun Modifier.neubrutalShadow(
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    color: Color = NeubrutalBlack
): Modifier = this.then(
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = color
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    this.color = color.toArgb()
                    setShadowLayer(
                        0f,
                        offsetX.toPx(),
                        offsetY.toPx(),
                        color.toArgb()
                    )
                }
            }
            val radiusPx = cornerRadius.toPx()
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = radiusPx,
                radiusY = radiusPx,
                paint = paint
            )
        }
    }
)

/**
 * Applies a neubrutalism card style:
 * - thick dark border (2.dp)
 * - hard offset shadow bottom-right (4.dp, 4.dp)
 * - white background
 * - rounded corners
 *
 * IMPORTANT: Apply .clip(RoundedCornerShape(cornerRadius)) AFTER this modifier
 * so the shadow can extend slightly beyond the clipped bounds.
 */
fun Modifier.neubrutalCard(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 2.dp,
    borderColor: Color = NeubrutalBlack,
    shadowColor: Color = NeubrutalBlack,
    shadowOffsetX: Dp = 4.dp,
    shadowOffsetY: Dp = 4.dp
): Modifier = this
    .neubrutalShadow(
        offsetX = shadowOffsetX,
        offsetY = shadowOffsetY,
        cornerRadius = cornerRadius,
        color = shadowColor
    )

/**
 * Press scale animation with spring physics.
 */
fun Modifier.neubrutalPressable(
    enabled: Boolean = true,
    pressScale: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) {
        if (onClick != null) clickable(onClick = onClick) else this
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) pressScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "neubrutal_press_scale"
        )

        this
            .scale(scale)
            .then(
                if (onClick != null) {
                    clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    }
}

/**
 * Combines neubrutal card + pressable for interactive surfaces.
 */
fun Modifier.neubrutalInteractiveCard(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 2.dp,
    borderColor: Color = NeubrutalBlack,
    shadowColor: Color = NeubrutalBlack,
    shadowOffsetX: Dp = 4.dp,
    shadowOffsetY: Dp = 4.dp,
    pressScale: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier = this
    .neubrutalCard(
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        borderColor = borderColor,
        shadowColor = shadowColor,
        shadowOffsetX = shadowOffsetX,
        shadowOffsetY = shadowOffsetY
    )
    .neubrutalPressable(
        enabled = onClick != null,
        pressScale = pressScale,
        onClick = onClick
    )
