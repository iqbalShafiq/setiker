package presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
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

/** Standard horizontal inset for screen scrollable content (lists, search bars, cards). */
val ScreenContentHorizontalPadding = 20.dp

/** Applies [ScreenContentHorizontalPadding] to the receiver. */
fun Modifier.screenContentHorizontalPadding(): Modifier =
    padding(horizontal = ScreenContentHorizontalPadding)

/**
 * Padded full-width content when the parent does **not** already apply
 * [screenContentHorizontalPadding] (e.g. [androidx.compose.foundation.layout.Column] sections).
 */
fun Modifier.screenContentWidth(): Modifier =
    screenContentHorizontalPadding().fillMaxWidth()

/**
 * Expands width to cancel a parent's [screenContentHorizontalPadding] so children can scroll
 * edge-to-edge. Pair with leading/trailing spacers in [presentation.components.ScreenContentHorizontalScrollRow].
 */
fun Modifier.breakOutOfParentHorizontalPadding(
    padding: Dp = ScreenContentHorizontalPadding
): Modifier = graphicsLayer { clip = false }.layout { measurable, constraints ->
    val paddingPx = padding.roundToPx()
    val outward = paddingPx * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.maxWidth + outward,
            maxWidth = constraints.maxWidth + outward
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-paddingPx, 0)
    }
}

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
 *
 * The geometry is built in a [drawWithCache] block: the previous implementation allocated two
 * [Path] objects and two gradient brushes **on every frame** for every bordered surface in the
 * app. During a scroll that is a lot of short-lived garbage for a shape that only changes when
 * the size changes.
 */
fun Modifier.neubrutalGlossyHighlight(
    cornerRadius: Dp = NeubrutalCardRadius,
    highlightColor: Color = NeubrutalGlossyHighlightLight,
    bandWidth: Dp = NeubrutalGlossyBandWidth,
    inset: Dp = NeubrutalGlossyInset
): Modifier = drawWithCache {
    val geometry = buildGlossGeometry(
        widthPx = size.width,
        heightPx = size.height,
        cornerRadiusPx = cornerRadius.toPx(),
        bandWidthPx = bandWidth.toPx(),
        insetPx = (inset.toPx() - 0.5f).coerceAtLeast(0f),
        highlightColor = highlightColor
    )

    onDrawBehind {
        val geo = geometry ?: return@onDrawBehind
        // Keep the filled bevel continuous; the gradient is only sheen, not the
        // base fill, so the right side no longer looks erased.
        //
        // The two extra gradient sheens were removed on purpose: they modulated the fill by
        // less than 10 % alpha over a 4 dp band, but they doubled the number of draw commands
        // for *every* bordered surface in the app. Profiling on a Redmi Note 11 showed the
        // render thread spending 5.5 ms per frame issuing draw commands while scrolling the
        // sticker grid — the single largest frame cost.
        drawPath(path = geo.bevelPath, color = geo.fillColor)
        drawPath(
            path = geo.rimPath,
            color = geo.rimColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = geo.rimWidth)
        )
    }
}

/**
 * The cached geometry of the glossy bevel.
 *
 * Built once per size change instead of once per frame.
 */
private class GlossGeometry(
    val bevelPath: Path,
    val rimPath: Path,
    val fillColor: Color,
    val rimColor: Color,
    val rimWidth: Float
)

private fun buildGlossGeometry(
    widthPx: Float,
    heightPx: Float,
    cornerRadiusPx: Float,
    bandWidthPx: Float,
    insetPx: Float,
    highlightColor: Color
): GlossGeometry? {
    val minSide = min(widthPx, heightPx)
    if (minSide <= 0f || widthPx <= insetPx * 2f || heightPx <= insetPx * 2f) return null

    val maxBandPx = minSide * 0.22f
    val minBandPx = 2.5f
    val preferredBandPx = min(bandWidthPx, minSide * 0.18f)
    val bandPx = if (maxBandPx < minBandPx) {
        maxBandPx
    } else {
        preferredBandPx.coerceIn(minBandPx, maxBandPx)
    }
    if (bandPx <= 0f) return null

    val radiusPx = min(cornerRadiusPx, minSide / 2f)
    val innerRadiusPx = (radiusPx - bandPx).coerceAtLeast(0f)
    val right = widthPx - insetPx
    val bottom = heightPx - insetPx
    val fillAlpha = (highlightColor.alpha * 0.82f).coerceAtMost(1f)

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

    val rimAlpha = (highlightColor.alpha * 0.9f).coerceAtMost(1f)
    val rimWidth = (bandPx * 0.32f).coerceAtLeast(1.25f)
    val rimPath = Path().apply {
        moveTo(right, insetPx + bandPx)
        lineTo(insetPx + bandPx, insetPx + bandPx)
        lineTo(insetPx + bandPx, bottom)
    }

    return GlossGeometry(
        bevelPath = bevelPath,
        rimPath = rimPath,
        fillColor = highlightColor.copy(alpha = fillAlpha),
        rimColor = highlightColor.copy(alpha = rimAlpha),
        rimWidth = rimWidth
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
