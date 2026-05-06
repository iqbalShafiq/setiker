package data.util

import android.content.Context
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
    android.util.Log.d("CropProcessor", "Starting crop transformation")
    android.util.Log.d("CropProcessor", "Source: $sourcePath")
    android.util.Log.d("CropProcessor", "Params: scale=$scale, rotation=$rotation, offset=($offsetX, $offsetY), flipH=$flipHorizontal, flipV=$flipVertical")

    // Load source bitmap
    val sourceBitmap = BitmapFactory.decodeFile(sourcePath)
        ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

    android.util.Log.d("CropProcessor", "Source image: ${sourceBitmap.width}x${sourceBitmap.height}")

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

    // Save to same directory as source file
    val sourceFile = File(sourcePath)
    val parentDir = sourceFile.parentFile ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
    val outputFile = File(parentDir, "cropped_${System.currentTimeMillis()}.webp")

    FileOutputStream(outputFile).use { out ->
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        outputBitmap.compress(format, 90, out)
    }

    android.util.Log.d("CropProcessor", "Output saved: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")

    // Cleanup
    if (sourceBitmap != outputBitmap) sourceBitmap.recycle()
    outputBitmap.recycle()

    outputFile.absolutePath
}
