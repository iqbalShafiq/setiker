package data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

actual class StickerFileStorage(private val context: Context) {

    private val stickersDir: File
        get() = File(context.filesDir, "stickers").apply { mkdirs() }

    actual suspend fun saveImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val destFile = File(stickersDir, fileName)
            File(sourcePath).inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        }

    actual suspend fun saveBytes(bytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val destFile = File(stickersDir, fileName)
            destFile.writeBytes(bytes)
            destFile.absolutePath
        }

    actual suspend fun loadImage(fileName: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = File(stickersDir, fileName)
            if (file.exists()) file.readBytes() else null
        }

    actual suspend fun deleteImage(fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            File(stickersDir, fileName).delete()
        }

    actual suspend fun getImagePath(fileName: String): String {
        // Always resolve against stickers directory using basename
        val basename = File(fileName).name
        return File(stickersDir, basename).absolutePath
    }

    actual suspend fun imageExists(fileName: String): Boolean =
        File(stickersDir, fileName).exists()

    actual suspend fun convertToWebP(sourcePath: String, outputFileName: String): String =
        withContext(Dispatchers.IO) {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                throw IllegalArgumentException("Source file does not exist: $sourcePath")
            }

            // If already WebP, just copy to stickers directory
            if (sourceFile.extension.equals("webp", ignoreCase = true)) {
                return@withContext saveImage(sourcePath, outputFileName)
            }

            // Convert to WebP 512x512
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                // Resize to 512x512 while maintaining aspect ratio
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

                val destFile = File(stickersDir, outputFileName)
                FileOutputStream(destFile).use { out ->
                    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    scaledBitmap.compress(format, 90, out)
                }

                // Cleanup
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    /**
     * Save tray icon for WhatsApp.
     * Requirements: 96x96px, max 50KB, PNG format
     */
    actual suspend fun saveTrayImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                // Resize to 96x96
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, true)

                // Compress as PNG, reduce quality if needed to stay under 50KB
                var quality = 100
                var bytes: ByteArray
                do {
                    val stream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
                    bytes = stream.toByteArray()
                    quality -= 10
                } while (bytes.size > 50 * 1024 && quality > 10)

                val destFile = File(stickersDir, fileName)
                destFile.writeBytes(bytes)

                android.util.Log.d("StickerFileStorage", 
                    "Tray icon saved: ${destFile.absolutePath}, size: ${bytes.size} bytes (${bytes.size / 1024}KB)")

                // Cleanup
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    /**
     * Save sticker image for WhatsApp.
     * Requirements: 512x512px, max 100KB, WebP format
     */
    actual suspend fun saveStickerImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                // Resize to 512x512
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

                // Compress as WebP, reduce quality if needed to stay under 100KB
                var quality = 90
                var bytes: ByteArray
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }

                do {
                    val stream = ByteArrayOutputStream()
                    scaledBitmap.compress(format, quality, stream)
                    bytes = stream.toByteArray()
                    quality -= 5
                } while (bytes.size > 100 * 1024 && quality > 20)

                val destFile = File(stickersDir, fileName)
                destFile.writeBytes(bytes)

                android.util.Log.d("StickerFileStorage", 
                    "Sticker saved: ${destFile.absolutePath}, size: ${bytes.size} bytes (${bytes.size / 1024}KB)")

                // Cleanup
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    actual suspend fun saveStickerImageWithDecorations(
        sourcePath: String,
        fileName: String,
        decorations: List<StickerDecoration>
    ): String = withContext(Dispatchers.IO) {
        if (decorations.isEmpty()) {
            return@withContext saveStickerImage(sourcePath, fileName)
        }

        val sourceBitmap = BitmapFactory.decodeFile(sourcePath)
            ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

        try {
            val composedBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(composedBitmap)
            val minDim = minOf(composedBitmap.width, composedBitmap.height).toFloat()

            decorations.forEach { decoration ->
                val centerX = decoration.centerX.coerceIn(0f, 1f) * composedBitmap.width
                val centerY = decoration.centerY.coerceIn(0f, 1f) * composedBitmap.height
                val scale = decoration.scale.coerceIn(
                    DecorationRenderSpec.MIN_SCALE,
                    DecorationRenderSpec.MAX_SCALE
                )
                when (decoration) {
                    is TextDecoration -> {
                        if (decoration.id.startsWith("api_txt_")) {
                            drawApiOutsideForegroundCaption(
                                canvas = canvas,
                                decoration = decoration,
                                bitmapWidth = composedBitmap.width,
                                bitmapHeight = composedBitmap.height,
                                minDim = minDim
                            )
                        } else {
                            drawTextDecoration(
                                canvas = canvas,
                                text = decoration.text,
                                centerX = centerX,
                                centerY = centerY,
                                textSize = minDim * DecorationRenderSpec.TEXT_SIZE_RATIO * scale,
                                typeface = mapTypeface(decoration.font, decoration.fontWeight),
                                textColor = decoration.textColorArgb.toInt()
                            )
                        }
                    }

                    is EmojiDecoration -> {
                        drawTextDecoration(
                            canvas = canvas,
                            text = decoration.emoji,
                            centerX = centerX,
                            centerY = centerY,
                            textSize = minDim * DecorationRenderSpec.EMOJI_SIZE_RATIO * scale,
                            typeface = Typeface.DEFAULT,
                            textColor = android.graphics.Color.WHITE
                        )
                    }

                    is ImageDecoration -> {
                        val stickerBitmap = BitmapFactory.decodeFile(decoration.imagePath) ?: return@forEach
                        val baseSize = minDim * DecorationRenderSpec.IMAGE_BASE_RATIO * scale
                        val aspectRatio = stickerBitmap.width.toFloat() / stickerBitmap.height.toFloat()
                        val drawWidth: Float
                        val drawHeight: Float
                        if (aspectRatio >= 1f) {
                            drawWidth = baseSize
                            drawHeight = baseSize / aspectRatio
                        } else {
                            drawHeight = baseSize
                            drawWidth = baseSize * aspectRatio
                        }
                        val targetRect = RectF(
                            centerX - drawWidth / 2f,
                            centerY - drawHeight / 2f,
                            centerX + drawWidth / 2f,
                            centerY + drawHeight / 2f
                        )
                        canvas.drawBitmap(stickerBitmap, null, targetRect, null)
                        stickerBitmap.recycle()
                    }
                }
            }

            val tempComposedFile = File(context.cacheDir, "composed_${System.currentTimeMillis()}.png")
            FileOutputStream(tempComposedFile).use { output ->
                composedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            composedBitmap.recycle()
            saveStickerImage(tempComposedFile.absolutePath, fileName).also {
                tempComposedFile.delete()
            }
        } finally {
            sourceBitmap.recycle()
        }
    }

    /**
     * Grid-split API captions: same geometry as Compose preview/editor — bounded width, anchored at [centerX],[centerY].
     */
    private fun drawApiOutsideForegroundCaption(
        canvas: Canvas,
        decoration: TextDecoration,
        bitmapWidth: Int,
        bitmapHeight: Int,
        minDim: Float
    ) {
        val scale = decoration.scale.coerceIn(
            DecorationRenderSpec.MIN_SCALE,
            DecorationRenderSpec.MAX_SCALE
        )
        val textSizePx = minDim * DecorationRenderSpec.API_CAPTION_TEXT_SIZE_RATIO * scale
        val boxWidthPx =
            bitmapWidth * (1f - 2f * DecorationRenderSpec.API_CAPTION_HORIZONTAL_INSET_RATIO)
        val maxWidth = boxWidthPx.toInt().coerceAtLeast(1)

        val centerXPx = decoration.centerX.coerceIn(0f, 1f) * bitmapWidth
        val centerYPx = decoration.centerY.coerceIn(0f, 1f) * bitmapHeight

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = decoration.textColorArgb.toInt()
            this.textSize = textSizePx
            typeface = mapTypeface(decoration.font, decoration.fontWeight)
            isAntiAlias = true
            setShadowLayer(textSizePx * 0.14f, 0f, 1f, android.graphics.Color.BLACK)
        }

        val staticLayout = StaticLayout.Builder.obtain(
            decoration.text,
            0,
            decoration.text.length,
            textPaint,
            maxWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .build()

        val layoutHeight = staticLayout.height.toFloat()
        var left = centerXPx - boxWidthPx / 2f
        var top = centerYPx - layoutHeight / 2f
        left = left.coerceIn(0f, (bitmapWidth - boxWidthPx).coerceAtLeast(0f))
        top = top.coerceIn(0f, (bitmapHeight - layoutHeight).coerceAtLeast(0f))

        canvas.save()
        canvas.translate(left, top)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawTextDecoration(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        textSize: Float,
        typeface: Typeface,
        textColor: Int
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(fillPaint).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = (textSize * 0.08f).coerceAtLeast(2f)
        }
        val baselineY = centerY - (fillPaint.descent() + fillPaint.ascent()) / 2f
        canvas.drawText(text, centerX, baselineY, strokePaint)
        canvas.drawText(text, centerX, baselineY, fillPaint)
    }

    private fun mapTypeface(font: DecorationFont, fontWeight: DecorationFontWeight): Typeface {
        val base = when (font) {
            DecorationFont.Sans -> Typeface.SANS_SERIF
            DecorationFont.Serif -> Typeface.SERIF
            DecorationFont.Mono -> Typeface.MONOSPACE
            DecorationFont.Cursive -> Typeface.create("cursive", Typeface.NORMAL)
            DecorationFont.Display -> Typeface.create("serif", Typeface.NORMAL)
            DecorationFont.Rounded -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            DecorationFont.Condensed -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        }
        val style = when (fontWeight) {
            DecorationFontWeight.Light -> Typeface.NORMAL
            DecorationFontWeight.Regular -> Typeface.NORMAL
            DecorationFontWeight.Medium -> Typeface.NORMAL
            DecorationFontWeight.SemiBold -> Typeface.BOLD
            DecorationFontWeight.Bold -> Typeface.BOLD
        }
        return Typeface.create(base, style)
    }
}
