package data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun applyCropTransformation(
    sourcePath: String,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    outputSize: Int
): String = withContext(Dispatchers.IO) {
    // Load source bitmap
    val sourceBitmap = BitmapFactory.decodeFile(sourcePath)
        ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

    // Create output bitmap
    val outputBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Build transformation matrix
    val matrix = Matrix()

    // Move to center
    matrix.postTranslate(-sourceBitmap.width / 2f, -sourceBitmap.height / 2f)

    // Apply transformations
    matrix.postScale(scale, scale)
    if (flipHorizontal) matrix.postScale(-1f, 1f)
    if (flipVertical) matrix.postScale(1f, -1f)
    matrix.postRotate(rotation)

    // Move to output center + offset
    matrix.postTranslate(outputSize / 2f + offsetX, outputSize / 2f + offsetY)

    // Draw
    canvas.drawBitmap(sourceBitmap, matrix, paint)

    // Save as WebP
    val outputFile = File(sourcePath).parentFile?.let {
        File(it, "cropped_${System.currentTimeMillis()}.webp")
    } ?: File("cropped_${System.currentTimeMillis()}.webp")

    FileOutputStream(outputFile).use { out ->
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        outputBitmap.compress(format, 90, out)
    }

    // Cleanup
    if (sourceBitmap != outputBitmap) sourceBitmap.recycle()
    outputBitmap.recycle()

    outputFile.absolutePath
}