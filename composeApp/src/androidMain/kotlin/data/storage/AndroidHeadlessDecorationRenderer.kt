package data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.TextDecorationLayout
import domain.model.isBottomCaption
import domain.model.resolveEmojiStyle
import domain.model.resolveStyle
import presentation.components.decorationTypeface
import kotlin.math.max
import kotlin.math.min

/**
 * Canvas-based decoration render that does **not** need a foreground Activity.
 * Used by background AI jobs when the user leaves the app mid-processing.
 *
 * Prefer [AndroidDecorationBitmapRenderer] (Compose overlay) when an Activity is available
 * for pixel-perfect parity with the editor; this is the reliable offline fallback.
 */
internal object AndroidHeadlessDecorationRenderer {

    fun render(
        context: Context,
        width: Int,
        height: Int,
        decorations: List<StickerDecoration>
    ): Bitmap {
        require(width > 0 && height > 0)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val minDim = min(width, height).toFloat()
        val widthF = width.toFloat()
        val heightF = height.toFloat()

        decorations.forEach { decoration ->
            val scale = decoration.scale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)
            val centerX = decoration.centerX.coerceIn(0f, 1f) * widthF
            val centerY = decoration.centerY.coerceIn(0f, 1f) * heightF
            when (decoration) {
                is TextDecoration -> drawText(context, canvas, decoration, centerX, centerY, widthF, minDim, scale)
                is EmojiDecoration -> drawEmoji(canvas, decoration, centerX, centerY, minDim, scale)
                is ImageDecoration -> drawImage(canvas, decoration, centerX, centerY, minDim, scale)
            }
        }
        return bitmap
    }

    private fun drawText(
        context: Context,
        canvas: Canvas,
        decoration: TextDecoration,
        centerX: Float,
        centerY: Float,
        canvasWidth: Float,
        minDim: Float,
        scale: Float
    ) {
        val text = decoration.text
        if (text.isBlank()) return
        val textSizePx = DecorationRenderSpec.textSizePx(decoration, minDim, scale)
        val resolved = decoration.resolveStyle(textSizePx)
        val typeface = decorationTypeface(context, resolved.font, resolved.fontWeight)
        val boxWidth = DecorationRenderSpec.textBoxWidthPx(decoration, canvasWidth, minDim, scale)
        val boxHeight = DecorationRenderSpec.textBoxHeightPx(decoration, minDim, scale)

        val measured = when (decoration.layout) {
            TextDecorationLayout.Arched -> {
                val arched = DecorationTextCanvasDrawer.measureArchedContentBoundsPx(
                    text = text,
                    textSize = textSizePx,
                    typeface = typeface,
                    resolved = resolved,
                    arcIntensity = decoration.arcIntensity
                )
                arched.widthPx to arched.heightPx
            }
            else -> {
                val maxLineWidth = boxWidth.toInt().coerceAtLeast(1)
                val flat = DecorationTextCanvasDrawer.measureFlatTextBoundsPx(
                    text = text,
                    textSize = textSizePx,
                    typeface = typeface,
                    resolved = resolved,
                    maxLineWidthPx = maxLineWidth
                )
                flat.widthPx to flat.heightPx
            }
        }

        val (itemW, itemH) = DecorationRenderSpec.textDecorationBoxSizePx(
            decoration = decoration,
            canvasWidthPx = canvasWidth,
            minDimPx = minDim,
            scale = scale,
            measuredTextWidthPx = measured.first,
            measuredTextHeightPx = measured.second,
            resolvedStyle = resolved
        )

        val left = centerX - itemW / 2f
        val top = centerY - itemH / 2f

        when {
            decoration.layout == TextDecorationLayout.Arched -> {
                DecorationTextCanvasDrawer.drawArchedTextInBox(
                    canvas = canvas,
                    text = text,
                    boxLeft = left,
                    boxTop = top,
                    boxWidth = itemW,
                    boxHeight = itemH,
                    textSize = textSizePx,
                    typeface = typeface,
                    resolved = resolved,
                    arcIntensity = decoration.arcIntensity
                )
            }
            decoration.isBottomCaption() || decoration.layout == TextDecorationLayout.Freeform -> {
                DecorationTextCanvasDrawer.drawFlatTextInBox(
                    canvas = canvas,
                    text = text,
                    boxLeft = left,
                    boxTop = top,
                    boxWidth = itemW,
                    boxHeight = itemH,
                    textSize = textSizePx,
                    typeface = typeface,
                    resolved = resolved
                )
            }
            else -> {
                DecorationTextCanvasDrawer.drawFlatTextInBox(
                    canvas = canvas,
                    text = text,
                    boxLeft = left,
                    boxTop = top,
                    boxWidth = itemW,
                    boxHeight = itemH,
                    textSize = textSizePx,
                    typeface = typeface,
                    resolved = resolved
                )
            }
        }
    }

    private fun drawEmoji(
        canvas: Canvas,
        decoration: EmojiDecoration,
        centerX: Float,
        centerY: Float,
        minDim: Float,
        scale: Float
    ) {
        val emoji = decoration.emoji
        if (emoji.isBlank()) return
        val textSizePx = minDim * DecorationRenderSpec.EMOJI_SIZE_RATIO * scale
        val resolved = decoration.resolveEmojiStyle(textSizePx)
        DecorationTextCanvasDrawer.drawSingleLine(
            canvas = canvas,
            text = emoji,
            centerX = centerX,
            centerY = centerY,
            textSize = textSizePx,
            typeface = Typeface.DEFAULT,
            resolved = resolved
        )
    }

    private fun drawImage(
        canvas: Canvas,
        decoration: ImageDecoration,
        centerX: Float,
        centerY: Float,
        minDim: Float,
        scale: Float
    ) {
        val path = decoration.imagePath
        if (path.isBlank()) return
        val source = runCatching {
            BitmapFactory.decodeFile(path)
        }.getOrNull() ?: return
        try {
            val baseSize = minDim * DecorationRenderSpec.IMAGE_BASE_RATIO * scale
            val aspect = if (source.height > 0) source.width.toFloat() / source.height else 1f
            val drawW: Float
            val drawH: Float
            if (aspect >= 1f) {
                drawW = baseSize
                drawH = baseSize / max(aspect, 0.01f)
            } else {
                drawH = baseSize
                drawW = baseSize * aspect
            }
            val dest = RectF(
                centerX - drawW / 2f,
                centerY - drawH / 2f,
                centerX + drawW / 2f,
                centerY + drawH / 2f
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(source, null, dest, paint)
        } finally {
            source.recycle()
        }
    }
}
