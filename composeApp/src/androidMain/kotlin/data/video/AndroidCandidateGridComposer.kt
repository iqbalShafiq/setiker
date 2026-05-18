package data.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import domain.model.CandidateGridImage
import domain.util.VideoStickerPackPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

class AndroidCandidateGridComposer(
    private val context: Context
) : CandidateGridComposer {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(210, 0, 0, 0) }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        isFakeBoldText = true
    }
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 232, 232) }

    override suspend fun composeGrids(candidatePaths: List<String>): List<CandidateGridImage> = withContext(Dispatchers.IO) {
        VideoStickerPackPlanner.batchCandidatePathsForGrids(candidatePaths).mapIndexed { gridIndex, batch ->
            val bitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val gutter = 12f
            val cellSize = (GRID_SIZE - gutter * 5) / 4f

            batch.forEachIndexed { index, path ->
                val row = index / 4
                val col = index % 4
                val left = gutter + col * (cellSize + gutter)
                val top = gutter + row * (cellSize + gutter)
                drawCell(canvas, path, RectF(left, top, left + cellSize, top + cellSize), labelFor(index))
            }

            val outDir = File(context.cacheDir, "video_pack_candidate_grids").apply { mkdirs() }
            val outFile = File(outDir, "candidate_grid_${Clock.System.now().toEpochMilliseconds()}_$gridIndex.png")
            val compressed = FileOutputStream(outFile).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()

            check(compressed) { "Failed to compress candidate grid bitmap" }
            CandidateGridImage(filePath = outFile.absolutePath, frameCount = batch.size)
        }
    }

    private fun drawCell(canvas: Canvas, imagePath: String, dest: RectF, label: String) {
        val bitmap = decodeSampledBitmap(imagePath)
        if (bitmap != null) {
            val source = centerCropSource(bitmap)
            canvas.drawBitmap(bitmap, source, dest, null)
            bitmap.recycle()
        } else {
            canvas.drawRect(dest, placeholderPaint)
        }

        canvas.drawRect(dest, borderPaint)

        val labelRect = RectF(dest.left + 10f, dest.top + 10f, dest.left + 78f, dest.top + 54f)
        canvas.drawRoundRect(labelRect, 8f, 8f, labelBgPaint)
        canvas.drawText(label, labelRect.left + 10f, labelRect.bottom - 12f, labelTextPaint)
    }

    private fun centerCropSource(bitmap: Bitmap): Rect {
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Rect(left, top, left + size, top + size)
    }

    private fun labelFor(index: Int): String {
        val row = listOf('A', 'B', 'C', 'D')[index / 4]
        val col = index % 4 + 1
        return "$row$col"
    }

    private fun decodeSampledBitmap(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        val sampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            targetSize = CELL_DECODE_TARGET
        )
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height

        while (currentWidth > targetSize || currentHeight > targetSize) {
            currentWidth /= 2
            currentHeight /= 2
            sampleSize *= 2
        }

        return sampleSize.coerceAtLeast(1)
    }

    private companion object {
        const val GRID_SIZE = 1536
        const val CELL_DECODE_TARGET = 512
    }
}
