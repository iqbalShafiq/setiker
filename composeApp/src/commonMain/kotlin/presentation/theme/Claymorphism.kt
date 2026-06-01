package presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

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

/** Thickness of the filled top/left glossy bevel band (not a hairline stroke). */
val NeubrutalGlossyBandWidth = 4.dp

/** Inset of the glossy bevel from the outer edge (tight to top-left inside the border). */
val NeubrutalGlossyInset = 1.dp

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
 * Glossy Inner Highlight (top-left bevel)
 * ============================================ */

/**
 * Draws a filled top/left inner bevel (L-shape) for the glossy raised look.
 * Apply after [clip], [background], and [border], before click handlers.
 */
fun Modifier.neubrutalGlossyHighlight(
    cornerRadius: Dp = NeubrutalCardRadius,
    highlightColor: Color = NeubrutalGlossyHighlightLight,
    bandWidth: Dp = NeubrutalGlossyBandWidth,
    inset: Dp = NeubrutalGlossyInset
): Modifier = drawBehind {
    drawNeubrutalGlossyHighlight(
        cornerRadius = cornerRadius,
        highlightColor = highlightColor,
        bandWidth = bandWidth,
        inset = inset
    )
}

/**
 * Thick neubrutal border plus the matching glossy highlight in one chain.
 */
fun Modifier.neubrutalBorderWithGloss(
    color: Color,
    cornerRadius: Dp = NeubrutalCardRadius,
    width: Dp = NeubrutalBorderWidth,
    shape: Shape = RoundedCornerShape(cornerRadius),
    highlightColor: Color = NeubrutalGlossyHighlightLight
): Modifier = this
    .border(width = width, color = color, shape = shape)
    .neubrutalGlossyHighlight(
        cornerRadius = cornerRadius,
        highlightColor = highlightColor
    )

internal fun DrawScope.drawNeubrutalGlossyHighlight(
    cornerRadius: Dp,
    highlightColor: Color,
    bandWidth: Dp,
    inset: Dp
) {
    // Nudge the bevel slightly toward the top-left so it hugs the inner border edge.
    val insetPx = (inset.toPx() - 0.5f).coerceAtLeast(0f)
    val minSide = min(size.width, size.height)
    if (minSide <= 0f || size.width <= insetPx * 2f || size.height <= insetPx * 2f) return

    val maxBandPx = minSide * 0.22f
    val minBandPx = 2.5f
    val preferredBandPx = min(bandWidth.toPx(), minSide * 0.18f)
    val bandPx = if (maxBandPx < minBandPx) {
        maxBandPx
    } else {
        preferredBandPx.coerceIn(minBandPx, maxBandPx)
    }
    if (bandPx <= 0f) return

    val radiusPx = min(cornerRadius.toPx(), minSide / 2f)
    val innerRadiusPx = (radiusPx - bandPx).coerceAtLeast(0f)
    val right = size.width - insetPx
    val bottom = size.height - insetPx
    val fillAlpha = (highlightColor.alpha * 0.82f).coerceAtMost(1f)
    val sheenAlpha = (highlightColor.alpha * 1.12f).coerceAtMost(1f)

    val bevelPath = Path().apply {
        if (radiusPx <= 0f) {
            moveTo(right, insetPx)
            lineTo(insetPx, insetPx)
            lineTo(insetPx, bottom)
            lineTo(insetPx + bandPx, bottom)
            lineTo(insetPx + bandPx, insetPx + bandPx)
            lineTo(right, insetPx + bandPx)
            close()
        } else {
            moveTo(right, insetPx)
            lineTo(insetPx + radiusPx, insetPx)
            arcTo(
                rect = Rect(
                    left = insetPx,
                    top = insetPx,
                    right = insetPx + radiusPx * 2f,
                    bottom = insetPx + radiusPx * 2f
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(insetPx, bottom)
            lineTo(insetPx + bandPx, bottom)
            if (innerRadiusPx > 0f) {
                lineTo(insetPx + bandPx, insetPx + bandPx + innerRadiusPx)
                arcTo(
                    rect = Rect(
                        left = insetPx + bandPx,
                        top = insetPx + bandPx,
                        right = insetPx + bandPx + innerRadiusPx * 2f,
                        bottom = insetPx + bandPx + innerRadiusPx * 2f
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } else {
                lineTo(insetPx + bandPx, insetPx + bandPx)
            }
            lineTo(right, insetPx + bandPx)
            close()
        }
    }

    // Keep the filled bevel continuous; the gradient is only sheen, not the
    // base fill, so the right side no longer looks erased.
    drawPath(
        path = bevelPath,
        color = highlightColor.copy(alpha = fillAlpha)
    )

    val topSheenBrush = Brush.verticalGradient(
        colors = listOf(
            highlightColor.copy(alpha = sheenAlpha),
            highlightColor.copy(alpha = fillAlpha * 0.52f)
        ),
        startY = insetPx,
        endY = insetPx + bandPx
    )
    drawPath(
        path = bevelPath,
        brush = topSheenBrush,
    )

    val leftSheenBrush = Brush.horizontalGradient(
        colors = listOf(
            highlightColor.copy(alpha = fillAlpha * 0.54f),
            highlightColor.copy(alpha = 0f)
        ),
        startX = insetPx,
        endX = insetPx + bandPx
    )
    drawPath(
        path = bevelPath,
        brush = leftSheenBrush,
    )

    val rimAlpha = (highlightColor.alpha * 0.9f).coerceAtMost(1f)
    val rimWidth = (bandPx * 0.32f).coerceAtLeast(1.25f)
    val rimPath = Path().apply {
        moveTo(right, insetPx + bandPx)
        lineTo(insetPx + bandPx, insetPx + bandPx)
        lineTo(insetPx + bandPx, bottom)
    }
    drawPath(
        path = rimPath,
        color = highlightColor.copy(alpha = rimAlpha),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = rimWidth)
    )
}

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
