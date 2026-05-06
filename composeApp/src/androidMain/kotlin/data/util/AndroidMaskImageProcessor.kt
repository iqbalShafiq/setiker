package data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
    val sourceBitmap = BitmapFactory.decodeFile(imagePath)
        ?: throw IllegalArgumentException("Cannot decode image: $imagePath")

    // Create mutable copy
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

    paths.forEach { brushPath ->
        paint.strokeWidth = brushPath.brushSize * kotlin.math.max(scaleX, scaleY)
        paint.xfermode = if (brushPath.isErasing) {
            PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            null // Restore mode - just draw normally (simplified)
        }

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

    // Save result
    val outputFile = File(imagePath).parentFile?.let {
        File(it, "masked_${System.currentTimeMillis()}.webp")
    } ?: File("masked_${System.currentTimeMillis()}.webp")

    FileOutputStream(outputFile).use { out ->
        resultBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out)
    }

    sourceBitmap.recycle()
    resultBitmap.recycle()

    outputFile.absolutePath
}