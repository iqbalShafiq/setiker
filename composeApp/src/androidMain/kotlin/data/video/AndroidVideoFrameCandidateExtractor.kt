package data.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import data.storage.StickerFileStorage
import domain.model.VideoFrameCandidate
import domain.util.VideoStickerPackPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.time.Clock

class AndroidVideoFrameCandidateExtractor(
    private val fileStorage: StickerFileStorage
) : VideoFrameCandidateExtractor {

    override suspend fun extractCandidates(
        videoPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<VideoFrameCandidate> = withContext(Dispatchers.IO) {
        val timestamps = VideoStickerPackPlanner.buildSampleTimestamps(startMs, endMs)
        val extracted = mutableListOf<VideoFrameCandidate>()
        var previousBrightness: Double? = null

        timestamps.forEachIndexed { index, timestamp ->
            val name = "video_pack_candidate_${Clock.System.now().toEpochMilliseconds()}_$index.png"
            val path = fileStorage.extractVideoFrameToFile(videoPath, timestamp, name)
            if (path != null) {
                val bitmap = decodeForAnalysis(path)
                if (bitmap != null) {
                    val brightness = scoreBrightness(bitmap)
                    val sharpness = scoreSharpness(bitmap)
                    val difference = previousBrightness?.let { abs(brightness - it) } ?: 0.0
                    previousBrightness = brightness
                    extracted += VideoFrameCandidate(
                        filePath = path,
                        timestampMs = timestamp,
                        sharpnessScore = sharpness,
                        brightnessScore = brightness,
                        differenceScore = difference
                    )
                    bitmap.recycle()
                }
            }
            onProgress(index + 1, timestamps.size)
        }

        val ranked = rankCandidates(extracted)
        val retained = ranked.mapTo(mutableSetOf()) { it.filePath }
        extracted
            .asSequence()
            .map { it.filePath }
            .filterNot { it in retained }
            .forEach { path -> File(path).delete() }

        ranked
    }

    private fun rankCandidates(candidates: List<VideoFrameCandidate>): List<VideoFrameCandidate> {
        val preferred = candidates
            .filter { it.brightnessScore in 0.12..0.92 }
            .filter { it.sharpnessScore >= 0.015 }
            .filter { it.differenceScore >= 0.02 }
            .sortedByDescending { it.sharpnessScore + it.differenceScore }
        val fallback = candidates.sortedByDescending { it.sharpnessScore + it.differenceScore }

        return (preferred + fallback)
            .distinctBy { it.filePath }
            .take(VideoStickerPackPlanner.MAX_CANDIDATES)
            .sortedBy { it.timestampMs }
    }

    private fun scoreBrightness(bitmap: Bitmap): Double {
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        var total = 0.0
        var count = 0
        var y = 0

        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                total += (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                count += 1
                x += stepX
            }
            y += stepY
        }

        return if (count == 0) 0.0 else total / count
    }

    private fun scoreSharpness(bitmap: Bitmap): Double {
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        var totalDiff = 0.0
        var count = 0
        var y = 0

        while (y < bitmap.height - stepY) {
            var x = 0
            while (x < bitmap.width - stepX) {
                val current = luminance(bitmap.getPixel(x, y))
                val right = luminance(bitmap.getPixel(x + stepX, y))
                val down = luminance(bitmap.getPixel(x, y + stepY))
                totalDiff += abs(current - right) + abs(current - down)
                count += 2
                x += stepX
            }
            y += stepY
        }

        return if (count == 0) 0.0 else totalDiff / count
    }

    private fun decodeForAnalysis(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, ANALYSIS_TARGET_SIZE)
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

    private fun luminance(color: Int): Double =
        (0.299 * Color.red(color) +
            0.587 * Color.green(color) +
            0.114 * Color.blue(color)) / 255.0

    private companion object {
        const val ANALYSIS_TARGET_SIZE = 256
    }
}
