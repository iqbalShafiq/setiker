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

internal object DecorationTextCanvasDrawer {

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

    fun drawArchedSingleLine(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        textSize: Float,
        typeface: android.graphics.Typeface,
        resolved: ResolvedTextDecorationStyle,
        arcIntensity: Float
    ) {
        if (text.isEmpty()) return
        val intensity = arcIntensity.coerceIn(-1f, 1f)
        if (intensity == 0f) {
            drawSingleLine(canvas, text, centerX, centerY, textSize, typeface, resolved)
            return
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        val charWidths = FloatArray(text.length) { index ->
            paint.measureText(text, index, index + 1)
        }
        val totalWidth = charWidths.sum()
        var xCursor = centerX - totalWidth / 2f
        val radius = (totalWidth / 2f).coerceAtLeast(textSize)
        val maxAngle = intensity * 0.55f

        text.forEachIndexed { index, char ->
            val charWidth = charWidths[index]
            val charCenterX = xCursor + charWidth / 2f
            val normalized = if (totalWidth > 0f) {
                ((charCenterX - centerX) / (totalWidth / 2f)).coerceIn(-1f, 1f)
            } else {
                0f
            }
            val angle = normalized * maxAngle
            val yOffset = radius * (1f - kotlin.math.cos(angle))
            val xOffset = radius * kotlin.math.sin(angle) * 0.15f
            val charString = char.toString()

            resolved.layers.forEach { layer ->
                drawSingleLineLayer(
                    canvas = canvas,
                    text = charString,
                    centerX = charCenterX + xOffset + layer.offsetXPx,
                    baselineY = centerY + yOffset + layer.offsetYPx,
                    textSize = textSize,
                    typeface = typeface,
                    layer = layer
                )
            }
            xCursor += charWidth
        }
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
            val paint = createTextPaint(textSize, typeface, layer)
            StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(0f, 1f)
                .build()
                .draw(canvas)
        }
    }

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
