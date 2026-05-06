package data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import presentation.backgroundremover.DrawPath
import java.io.File
import java.io.FileOutputStream

actual suspend fun applyMaskToImage(
    imagePath: String,
    paths: List<DrawPath>,
    canvasSize: IntSize
): String = withContext(Dispatchers.IO) {
    android.util.Log.d("MaskProcessor", "Starting mask application")
    android.util.Log.d("MaskProcessor", "Image: $imagePath")
    android.util.Log.d("MaskProcessor", "Canvas size: ${canvasSize.width}x${canvasSize.height}")
    android.util.Log.d("MaskProcessor", "Paths: ${paths.size}")

    val sourceBitmap = BitmapFactory.decodeFile(imagePath)
        ?: throw IllegalArgumentException("Cannot decode image: $imagePath")

    android.util.Log.d("MaskProcessor", "Source image: ${sourceBitmap.width}x${sourceBitmap.height}")

    // Create mutable copy with transparency support
    val resultBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Scale factor between canvas size and bitmap size
    val scaleX = sourceBitmap.width.toFloat() / canvasSize.width
    val scaleY = sourceBitmap.height.toFloat() / canvasSize.height

    android.util.Log.d("MaskProcessor", "Scale factors: $scaleX, $scaleY")

    paths.forEachIndexed { index, brushPath ->
        paint.strokeWidth = brushPath.brushSize * kotlin.math.max(scaleX, scaleY)
        paint.xfermode = if (brushPath.isErasing) {
            PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            null // Restore mode - just draw normally (simplified)
        }

        android.util.Log.d("MaskProcessor", "Path $index: ${brushPath.points.size} points, erasing=${brushPath.isErasing}, brushSize=${brushPath.brushSize}")

        if (brushPath.points.size > 1) {
            for (i in 1 until brushPath.points.size) {
                val start = brushPath.points[i - 1]
                val end = brushPath.points[i]
                canvas.drawLine(
                    start.first * scaleX,
                    start.second * scaleY,
                    end.first * scaleX,
                    end.second * scaleY,
                    paint
                )
            }
        }
    }

    paint.xfermode = null

    // Save result to same directory as source file
    val sourceFile = File(imagePath)
    val parentDir = sourceFile.parentFile ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
    val outputFile = File(parentDir, "masked_${System.currentTimeMillis()}.webp")

    FileOutputStream(outputFile).use { out ->
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        resultBitmap.compress(format, 90, out)
    }

    android.util.Log.d("MaskProcessor", "Output saved: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")

    sourceBitmap.recycle()
    resultBitmap.recycle()

    outputFile.absolutePath
}
