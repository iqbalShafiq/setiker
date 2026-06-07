package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import domain.model.DecorationRenderSpec
import domain.model.ResolvedTextDecorationStyle
import domain.model.ResolvedTextEffectLayer
import domain.model.TextDecoration
import domain.model.TextDecorationLayout
import domain.model.resolveStyle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class TextDecorationBoxMetrics(
    val widthPx: Float,
    val heightPx: Float,
    val textSizePx: Float,
    val baseStyle: TextStyle,
    val resolvedStyle: ResolvedTextDecorationStyle,
    val archedLayoutSpec: ArchedTextLayoutSpec? = null
)

data class ArchedGlyphPlacement(
    val char: String,
    val layer: ResolvedTextEffectLayer,
    val xPx: Float,
    val yPx: Float
)

data class ArchedTextLayoutSpec(
    val placements: List<ArchedGlyphPlacement>,
    val widthPx: Float,
    val heightPx: Float
)

data class FlatTextBoundsPx(
    val widthPx: Float,
    val heightPx: Float
)

fun measureFlatTextBoundsPx(
    text: String,
    baseStyle: TextStyle,
    textMeasurer: TextMeasurer
): FlatTextBoundsPx {
    if (text.isEmpty()) return FlatTextBoundsPx(0f, 0f)

    val lines = text.split('\n')
    var maxLineWidth = 0f
    var totalHeight = 0f
    lines.forEach { line ->
        val measureText = line.ifEmpty { " " }
        val layout = textMeasurer.measure(measureText, baseStyle)
        maxLineWidth = max(maxLineWidth, layout.size.width.toFloat())
        totalHeight += layout.size.height.toFloat()
    }
    return FlatTextBoundsPx(widthPx = maxLineWidth, heightPx = totalHeight)
}

fun computeArchedTextLayoutSpec(
    text: String,
    baseStyle: TextStyle,
    resolvedStyle: ResolvedTextDecorationStyle,
    arcIntensity: Float,
    textSizePx: Float,
    textMeasurer: TextMeasurer
): ArchedTextLayoutSpec {
    if (text.isEmpty()) {
        return ArchedTextLayoutSpec(emptyList(), 0f, 0f)
    }

    val intensity = arcIntensity.coerceIn(-1f, 1f)
    val lines = text.split('\n')
    val allPlacements = mutableListOf<ArchedGlyphPlacement>()
    var blockMinX = Float.POSITIVE_INFINITY
    var blockMinY = Float.POSITIVE_INFINITY
    var blockMaxX = Float.NEGATIVE_INFINITY
    var blockMaxY = Float.NEGATIVE_INFINITY
    var yLineOffset = 0f

    lines.forEach { line ->
        val lineSpec = computeSingleLineArchedSpec(
            line = line,
            baseStyle = baseStyle,
            resolvedStyle = resolvedStyle,
            intensity = intensity,
            textSizePx = textSizePx,
            textMeasurer = textMeasurer,
            yLineOffsetPx = yLineOffset
        )
        allPlacements += lineSpec.placements
        blockMinX = min(blockMinX, lineSpec.minX)
        blockMinY = min(blockMinY, lineSpec.minY)
        blockMaxX = max(blockMaxX, lineSpec.maxX)
        blockMaxY = max(blockMaxY, lineSpec.maxY)
        yLineOffset += lineSpec.lineAdvancePx
    }

    if (allPlacements.isEmpty()) {
        val flat = measureFlatTextBoundsPx(text, baseStyle, textMeasurer)
        return ArchedTextLayoutSpec(emptyList(), flat.widthPx, flat.heightPx)
    }

    val shiftX = blockMinX
    val shiftY = blockMinY
    val widthPx = blockMaxX - blockMinX
    val heightPx = blockMaxY - blockMinY
    val normalizedPlacements = allPlacements.map { placement ->
        placement.copy(
            xPx = placement.xPx - shiftX,
            yPx = placement.yPx - shiftY
        )
    }

    return ArchedTextLayoutSpec(
        placements = normalizedPlacements,
        widthPx = widthPx.coerceAtLeast(1f),
        heightPx = heightPx.coerceAtLeast(1f)
    )
}

private data class SingleLineArchedSpec(
    val placements: List<ArchedGlyphPlacement>,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val lineAdvancePx: Float
)

private fun computeSingleLineArchedSpec(
    line: String,
    baseStyle: TextStyle,
    resolvedStyle: ResolvedTextDecorationStyle,
    intensity: Float,
    textSizePx: Float,
    textMeasurer: TextMeasurer,
    yLineOffsetPx: Float
): SingleLineArchedSpec {
    val measureText = line.ifEmpty { " " }
    val lineLayout = textMeasurer.measure(measureText, baseStyle)
    val lineHeightPx = lineLayout.size.height.toFloat()
    val lineAdvancePx = lineHeightPx * 1.15f

    if (line.isEmpty()) {
        return SingleLineArchedSpec(
            placements = emptyList(),
            minX = 0f,
            minY = yLineOffsetPx,
            maxX = 0f,
            maxY = yLineOffsetPx + lineHeightPx,
            lineAdvancePx = lineAdvancePx
        )
    }

    if (intensity == 0f) {
        val flat = measureFlatTextBoundsPx(line, baseStyle, textMeasurer)
        return SingleLineArchedSpec(
            placements = emptyList(),
            minX = 0f,
            minY = yLineOffsetPx,
            maxX = flat.widthPx,
            maxY = yLineOffsetPx + flat.heightPx,
            lineAdvancePx = lineAdvancePx
        )
    }

    val totalWidthPx = lineLayout.size.width.toFloat()
    val radius = (totalWidthPx / 2f).coerceAtLeast(textSizePx)
    val maxAngle = intensity * 0.55f
    val placements = mutableListOf<ArchedGlyphPlacement>()
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    var xCursor = 0f
    line.forEach { char ->
        val charText = char.toString()
        val charLayout = textMeasurer.measure(charText, baseStyle)
        val charWidthPx = charLayout.size.width.toFloat()
        val charHeightPx = charLayout.size.height.toFloat()
        val charCenterX = xCursor + charWidthPx / 2f
        val normalized = if (totalWidthPx > 0f) {
            ((charCenterX - totalWidthPx / 2f) / (totalWidthPx / 2f)).coerceIn(-1f, 1f)
        } else {
            0f
        }
        val angle = normalized * maxAngle
        val yOffsetPx = radius * (1f - cos(angle))
        val xOffsetPx = radius * sin(angle) * 0.15f

        resolvedStyle.layers.forEach { layer ->
            val xPx = charCenterX + xOffsetPx + layer.offsetXPx
            val yPx = yLineOffsetPx + yOffsetPx + layer.offsetYPx
            placements += ArchedGlyphPlacement(charText, layer, xPx, yPx)

            val pad = max(
                if (!layer.isFill) layer.strokeWidthPx else 0f,
                layer.shadowBlurPx
            )
            minX = min(minX, xPx - pad)
            minY = min(minY, yPx - pad)
            maxX = max(maxX, xPx + charWidthPx + pad)
            maxY = max(maxY, yPx + charHeightPx + pad)
        }
        xCursor += charWidthPx
    }

    if (placements.isEmpty()) {
        return SingleLineArchedSpec(
            placements = emptyList(),
            minX = 0f,
            minY = yLineOffsetPx,
            maxX = totalWidthPx,
            maxY = yLineOffsetPx + lineHeightPx,
            lineAdvancePx = lineAdvancePx
        )
    }

    return SingleLineArchedSpec(
        placements = placements,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        lineAdvancePx = lineAdvancePx
    )
}

@Composable
fun rememberTextDecorationBoxMetrics(
    decoration: TextDecoration,
    minDim: Float,
    canvasWidthPx: Float,
    scale: Float
): TextDecorationBoxMetrics {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textSizePx = DecorationRenderSpec.textSizePx(decoration, minDim, scale)
    val resolvedStyle = decoration.resolveStyle(textSizePx)
    val baseStyle = TextStyle(
        fontFamily = decorationFontFamily(resolvedStyle.font, resolvedStyle.fontWeight),
        fontWeight = mapDecorationFontWeight(resolvedStyle.fontWeight),
        fontSize = with(density) { textSizePx.toSp() },
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    return remember(
        decoration.id,
        decoration.text,
        decoration.font,
        decoration.fontWeight,
        decoration.style,
        decoration.layout,
        decoration.arcIntensity,
        decoration.textColorArgb,
        minDim,
        canvasWidthPx,
        scale
    ) {
        val baseWidth = DecorationRenderSpec.textBoxWidthPx(
            decoration = decoration,
            canvasWidthPx = canvasWidthPx,
            minDimPx = minDim,
            scale = scale
        )
        val baseHeight = DecorationRenderSpec.textBoxHeightPx(
            decoration = decoration,
            minDimPx = minDim,
            scale = scale
        )
        val strokePad = resolvedStyle.layers.maxOfOrNull { layer ->
            if (!layer.isFill) layer.strokeWidthPx else 0f
        } ?: 0f
        val layerOffsetPad = resolvedStyle.layers.maxOfOrNull { layer ->
            max(layer.offsetXPx, layer.offsetYPx)
        } ?: 0f
        val padding = strokePad * 2f + layerOffsetPad

        if (decoration.layout == TextDecorationLayout.Arched && decoration.arcIntensity != 0f) {
            val arched = computeArchedTextLayoutSpec(
                text = decoration.text,
                baseStyle = baseStyle,
                resolvedStyle = resolvedStyle,
                arcIntensity = decoration.arcIntensity,
                textSizePx = textSizePx,
                textMeasurer = textMeasurer
            )
            TextDecorationBoxMetrics(
                widthPx = max(baseWidth, arched.widthPx + padding),
                heightPx = max(baseHeight, arched.heightPx + padding),
                textSizePx = textSizePx,
                baseStyle = baseStyle,
                resolvedStyle = resolvedStyle,
                archedLayoutSpec = arched
            )
        } else {
            val flat = measureFlatTextBoundsPx(
                text = decoration.text,
                baseStyle = baseStyle,
                textMeasurer = textMeasurer
            )
            TextDecorationBoxMetrics(
                widthPx = max(baseWidth, flat.widthPx + padding),
                heightPx = max(baseHeight, flat.heightPx + padding),
                textSizePx = textSizePx,
                baseStyle = baseStyle,
                resolvedStyle = resolvedStyle,
                archedLayoutSpec = null
            )
        }
    }
}
