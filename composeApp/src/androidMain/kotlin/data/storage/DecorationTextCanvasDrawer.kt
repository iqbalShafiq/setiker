package data.storage

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import domain.model.ResolvedTextDecorationStyle
import domain.model.ResolvedTextEffectLayer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal object DecorationTextCanvasDrawer {

    data class FlatTextBoundsPx(
        val widthPx: Float,
        val heightPx: Float
    )

    data class ArchedContentBoundsPx(
        val widthPx: Float,
        val heightPx: Float
    )

    fun measureFlatTextBoundsPx(
        text: String,
        textSize: Float,
        typeface: android.graphics.Typeface,
        resolved: ResolvedTextDecorationStyle,
        maxLineWidthPx: Int? = null
    ): FlatTextBoundsPx {
        if (text.isEmpty()) return FlatTextBoundsPx(0f, 0f)

        val paint = createTextPaint(textSize, typeface, resolved.layers.last())
        val lines = text.split('\n')
        var maxWidth = 0f
        var totalHeight = 0f
        lines.forEach { line ->
            val measureText = line.ifEmpty { " " }
            if (maxLineWidthPx != null) {
                val layout = StaticLayout.Builder.obtain(measureText, 0, measureText.length, paint, maxLineWidthPx)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1f)
                    .build()
                maxWidth = max(maxWidth, layout.width.toFloat())
                totalHeight += layout.height.toFloat()
            } else {
                maxWidth = max(maxWidth, paint.measureText(measureText))
                val metrics = paint.fontMetrics
                totalHeight += metrics.descent - metrics.ascent
            }
        }
        return FlatTextBoundsPx(widthPx = maxWidth, heightPx = totalHeight)
    }

    fun measureArchedContentBoundsPx(
        text: String,
        textSize: Float,
        typeface: android.graphics.Typeface,
        resolved: ResolvedTextDecorationStyle,
        arcIntensity: Float
    ): ArchedContentBoundsPx {
        if (text.isEmpty()) return ArchedContentBoundsPx(0f, 0f)

        val intensity = arcIntensity.coerceIn(-1f, 1f)
        if (intensity == 0f) {
            val flat = measureFlatTextBoundsPx(text, textSize, typeface, resolved)
            return ArchedContentBoundsPx(flat.widthPx, flat.heightPx)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        val metrics = paint.fontMetrics
        val lineHeightPx = metrics.descent - metrics.ascent
        val lines = text.split('\n')
        var blockMinX = Float.POSITIVE_INFINITY
        var blockMinY = Float.POSITIVE_INFINITY
        var blockMaxX = Float.NEGATIVE_INFINITY
        var blockMaxY = Float.NEGATIVE_INFINITY
        var yLineOffset = 0f

        lines.forEach { line ->
            if (line.isEmpty()) {
                yLineOffset += lineHeightPx * 1.15f
                return@forEach
            }
            val charWidths = FloatArray(line.length) { index ->
                paint.measureText(line, index, index + 1)
            }
            val totalWidth = charWidths.sum()
            var xCursor = 0f
            val radius = (totalWidth / 2f).coerceAtLeast(textSize)
            val maxAngle = intensity * 0.55f

            line.forEachIndexed { index, _ ->
                val charWidth = charWidths[index]
                val charCenterX = xCursor + charWidth / 2f
                val normalized = if (totalWidth > 0f) {
                    ((charCenterX - totalWidth / 2f) / (totalWidth / 2f)).coerceIn(-1f, 1f)
                } else {
                    0f
                }
                val angle = normalized * maxAngle
                val yOffset = radius * (1f - cos(angle))
                val xOffset = radius * sin(angle) * 0.15f

                resolved.layers.forEach { layer ->
                    val xPx = charCenterX + xOffset + layer.offsetXPx
                    val yPx = yLineOffset + yOffset + layer.offsetYPx
                    val pad = max(
                        if (!layer.isFill) layer.strokeWidthPx else 0f,
                        layer.shadowBlurPx
                    )
                    blockMinX = min(blockMinX, xPx - pad)
                    blockMinY = min(blockMinY, yPx - pad)
                    blockMaxX = max(blockMaxX, xPx + charWidth + pad)
                    blockMaxY = max(blockMaxY, yPx + lineHeightPx + pad)
                }
                xCursor += charWidth
            }
            yLineOffset += lineHeightPx * 1.15f
        }

        if (blockMinX == Float.POSITIVE_INFINITY) {
            val flat = measureFlatTextBoundsPx(text, textSize, typeface, resolved)
            return ArchedContentBoundsPx(flat.widthPx, flat.heightPx)
        }

        return ArchedContentBoundsPx(
            widthPx = (blockMaxX - blockMinX).coerceAtLeast(1f),
            heightPx = (blockMaxY - blockMinY).coerceAtLeast(1f)
        )
    }

    fun drawSingleLine(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        textSize: Float,
        typeface: android.graphics.Typeface,
        resolved: ResolvedTextDecorationStyle
    ) {
        val metricsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        val baselineY = centerY - (metricsPaint.descent() + metricsPaint.ascent()) / 2f
        resolved.layers.forEach { layer ->
            drawSingleLineLayer(
                canvas = canvas,
                text = text,
                centerX = centerX,
                baselineY = baselineY,
                textSize = textSize,
                typeface = typeface,
                layer = layer
            )
        }
    }

    fun drawFlatTextInBox(
        canvas: Canvas,
        text: String,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float,
        textSize: Float,
        typeface: android.graphics.Typeface,
        resolved: ResolvedTextDecorationStyle
    ) {
        val maxWidth = boxWidth.toInt().coerceAtLeast(1)
        // Each style layer is centered independently (matches StyledDecorationText in Compose).
        resolved.layers.forEach { layer ->
            val layout = createStaticLayout(
                text = text,
                textSize = textSize,
                typeface = typeface,
                layer = layer,
                maxWidth = maxWidth
            )
            val layerLeft = boxLeft + (boxWidth - layout.width) / 2f + layer.offsetXPx
            val layerTop = boxTop + (boxHeight - layout.height) / 2f + layer.offsetYPx
            canvas.save()
            canvas.translate(layerLeft, layerTop)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    fun drawArchedTextInBox(
        canvas: Canvas,
        text: String,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float,
        textSize: Float,
        typeface: android.graphics.Typeface,
        resolved: ResolvedTextDecorationStyle,
        arcIntensity: Float
    ) {
        if (text.isEmpty()) return
        val intensity = arcIntensity.coerceIn(-1f, 1f)
        if (intensity == 0f) {
            drawFlatTextInBox(
                canvas = canvas,
                text = text,
                boxLeft = boxLeft,
                boxTop = boxTop,
                boxWidth = boxWidth,
                boxHeight = boxHeight,
                textSize = textSize,
                typeface = typeface,
                resolved = resolved
            )
            return
        }

        val contentBounds = measureArchedContentBoundsPx(text, textSize, typeface, resolved, intensity)
        val contentLeft = boxLeft + (boxWidth - contentBounds.widthPx) / 2f
        val contentTop = boxTop + (boxHeight - contentBounds.heightPx) / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        val metrics = paint.fontMetrics
        val lineHeightPx = metrics.descent - metrics.ascent
        val lines = text.split('\n')

        var blockMinX = Float.POSITIVE_INFINITY
        var blockMinY = Float.POSITIVE_INFINITY
        val glyphDraws = mutableListOf<GlyphDraw>()
        var yLineOffset = 0f

        lines.forEach { line ->
            if (line.isEmpty()) {
                yLineOffset += lineHeightPx * 1.15f
                return@forEach
            }
            val charWidths = FloatArray(line.length) { index ->
                paint.measureText(line, index, index + 1)
            }
            val totalWidth = charWidths.sum()
            var xCursor = 0f
            val radius = (totalWidth / 2f).coerceAtLeast(textSize)
            val maxAngle = intensity * 0.55f

            line.forEachIndexed { index, char ->
                val charWidth = charWidths[index]
                val charCenterX = xCursor + charWidth / 2f
                val normalized = if (totalWidth > 0f) {
                    ((charCenterX - totalWidth / 2f) / (totalWidth / 2f)).coerceIn(-1f, 1f)
                } else {
                    0f
                }
                val angle = normalized * maxAngle
                val yOffset = radius * (1f - cos(angle))
                val xOffset = radius * sin(angle) * 0.15f

                resolved.layers.forEach { layer ->
                    val xPx = charCenterX + xOffset + layer.offsetXPx
                    val yPx = yLineOffset + yOffset + layer.offsetYPx
                    blockMinX = min(blockMinX, xPx)
                    blockMinY = min(blockMinY, yPx)
                    glyphDraws += GlyphDraw(
                        char = char.toString(),
                        centerX = xPx,
                        topY = yPx,
                        layer = layer
                    )
                }
                xCursor += charWidth
            }
            yLineOffset += lineHeightPx * 1.15f
        }

        if (glyphDraws.isEmpty()) return

        val shiftX = blockMinX
        val shiftY = blockMinY
        canvas.save()
        canvas.translate(contentLeft - shiftX, contentTop - shiftY)
        glyphDraws.forEach { glyph ->
            val layerPaint = createPaint(textSize, typeface, glyph.layer)
            val baselineY = glyph.topY - layerPaint.ascent()
            canvas.drawText(glyph.char, glyph.centerX, baselineY, layerPaint)
        }
        canvas.restore()
    }

    fun drawMultilineBlock(
        canvas: Canvas,
        text: String,
        maxWidth: Int,
        resolved: ResolvedTextDecorationStyle,
        textSize: Float,
        typeface: android.graphics.Typeface
    ) {
        resolved.layers.forEach { layer ->
            val layout = createStaticLayout(
                text = text,
                textSize = textSize,
                typeface = typeface,
                layer = layer,
                maxWidth = maxWidth
            )
            layout.draw(canvas)
        }
    }

    fun measureMultilineHeight(
        text: String,
        maxWidth: Int,
        textSizePx: Float,
        typeface: android.graphics.Typeface,
        resolved: ResolvedTextDecorationStyle
    ): Float {
        val fillLayer = resolved.layers.lastOrNull() ?: return textSizePx
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillLayer.colorArgb.toInt()
            this.textSize = textSizePx
            this.typeface = typeface
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .build()
            .height
            .toFloat()
    }

    private data class GlyphDraw(
        val char: String,
        val centerX: Float,
        val topY: Float,
        val layer: ResolvedTextEffectLayer
    )

    private fun drawSingleLineLayer(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        textSize: Float,
        typeface: android.graphics.Typeface,
        layer: ResolvedTextEffectLayer
    ) {
        val paint = createPaint(textSize, typeface, layer)
        canvas.drawText(text, centerX, baselineY, paint)
    }

    private fun createStaticLayout(
        text: String,
        textSize: Float,
        typeface: android.graphics.Typeface,
        layer: ResolvedTextEffectLayer,
        maxWidth: Int
    ): StaticLayout {
        val paint = createTextPaint(textSize, typeface, layer)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .build()
    }

    private fun createTextPaint(
        textSize: Float,
        typeface: android.graphics.Typeface,
        layer: ResolvedTextEffectLayer
    ): TextPaint = TextPaint(createPaint(textSize, typeface, layer))

    private fun createPaint(
        textSize: Float,
        typeface: android.graphics.Typeface,
        layer: ResolvedTextEffectLayer
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = layer.colorArgb.toInt()
        this.textSize = textSize
        this.typeface = typeface
        textAlign = Paint.Align.CENTER
        if (!layer.isFill && layer.strokeWidthPx > 0f) {
            style = Paint.Style.STROKE
            strokeWidth = layer.strokeWidthPx
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        } else {
            style = Paint.Style.FILL
            val gradientStart = layer.gradientStartArgb
            val gradientEnd = layer.gradientEndArgb
            if (gradientStart != null && gradientEnd != null) {
                shader = LinearGradient(
                    0f,
                    -textSize,
                    0f,
                    textSize,
                    intArrayOf(gradientStart.toInt(), gradientEnd.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
        }
        if (layer.shadowBlurPx > 0f) {
            setShadowLayer(layer.shadowBlurPx, 0f, layer.shadowOffsetYPx, layer.colorArgb.toInt())
        }
    }
}
