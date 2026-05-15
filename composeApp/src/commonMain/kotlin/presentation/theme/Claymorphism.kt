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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================
 * Neubrutalism Design Tokens
 * ============================================
 *
 * From reference image analysis:
 * - Buttons: larger radius (16.dp), thick border, 4x4 shadow
 * - Cards: smaller radius (12.dp), thick border, 4x4 shadow
 * - Small items (chips, tags): 8.dp radius, 2x2 shadow
 * - Dialogs: 20-24.dp radius, 6x6 shadow
 * - Shadow is a HARD solid offset shape (not blur)
 */

/** Button corner radius — most rounded interactive element. */
val NeubrutalButtonRadius = 16.dp

/** Card corner radius — slightly more square than buttons. */
val NeubrutalCardRadius = 12.dp

/** Small item radius (chips, tags, thumbnails). */
val NeubrutalSmallRadius = 8.dp

/** Dialog / sheet radius. */
val NeubrutalDialogRadius = 20.dp

/** Standard thick border width. */
val NeubrutalBorderWidth = 2.5.dp

/** Thin border for small elements. */
val NeubrutalThinBorderWidth = 1.5.dp

/** Standard shadow offset. */
val NeubrutalShadowOffset = 4.dp

/** Small shadow offset. */
val NeubrutalSmallShadowOffset = 2.dp

/** Large shadow offset (dialogs). */
val NeubrutalLargeShadowOffset = 6.dp

/* ============================================
 * Hard Offset Shadow Modifier
 * ============================================ */

/**
 * Draws a true neubrutal hard-offset shadow behind the content.
 *
 * The shadow is a solid-filled rounded rectangle shifted by [offsetX] / [offsetY]
 * with no blur — this is the signature neubrutal look.
 *
 * Apply **before** [clip] so the shadow can draw outside the clipped bounds.
 */
fun Modifier.neubrutalShadow(
    offsetX: Dp = NeubrutalShadowOffset,
    offsetY: Dp = NeubrutalShadowOffset,
    cornerRadius: Dp = NeubrutalCardRadius,
    color: Color = NeubrutalBlack
): Modifier = this.then(
    drawBehind {
        val radiusPx = cornerRadius.toPx()
        val offsetXPx = offsetX.toPx()
        val offsetYPx = offsetY.toPx()

        // Draw the hard shadow as a solid rounded rect behind the content.
        drawRoundRect(
            color = color,
            topLeft = Offset(offsetXPx, offsetYPx),
            size = androidx.compose.ui.geometry.Size(
                width = size.width,
                height = size.height
            ),
            cornerRadius = CornerRadius(radiusPx, radiusPx)
        )
    }
)

/* ============================================
 * Press Scale Animation
 * ============================================ */

/**
 * Press scale animation with spring physics.
 * Includes optional [onClick].
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

/* ============================================
 * Combined Card + Pressable
 * ============================================ */

/**
 * Combines neubrutal hard shadow + pressable behaviour for interactive cards.
 */
fun Modifier.neubrutalInteractiveCard(
    cornerRadius: Dp = NeubrutalCardRadius,
    shadowColor: Color = NeubrutalBlack,
    shadowOffsetX: Dp = NeubrutalShadowOffset,
    shadowOffsetY: Dp = NeubrutalShadowOffset,
    pressScale: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier = this
    .neubrutalShadow(
        offsetX = shadowOffsetX,
        offsetY = shadowOffsetY,
        cornerRadius = cornerRadius,
        color = shadowColor
    )
    .neubrutalPressable(
        enabled = onClick != null,
        pressScale = pressScale,
        onClick = onClick
    )
