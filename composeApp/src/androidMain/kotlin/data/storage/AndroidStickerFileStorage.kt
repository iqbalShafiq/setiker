package data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.concurrent.futures.await
import androidx.media3.common.MediaItem
import androidx.media3.inspector.frame.FrameExtractor
import data.storage.webp.WebpContainer
import domain.model.AnimatedStickerSpec
import domain.model.CropTransform
import domain.model.DecodedFrame
import domain.model.StickerDecoration
import domain.model.StickerPack
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    actual suspend fun readBytesAtPath(absolutePath: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = File(absolutePath)
            if (file.exists()) file.readBytes() else null
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

            if (sourceFile.extension.equals("webp", ignoreCase = true)) {
                return@withContext saveImage(sourcePath, outputFileName)
            }

            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                val scaledBitmap = resizeAndFitTo512(bitmap)

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

                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    actual suspend fun trySaveTrayImage(sourcePath: String, fileName: String): String? =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath) ?: return@withContext null
            try {
                val bytes = compressTrayBitmapForWhatsApp(bitmap) ?: return@withContext null
                val destFile = File(stickersDir, fileName)
                destFile.writeBytes(bytes)
                Log.d(
                    "StickerFileStorage",
                    "Tray icon saved: ${destFile.absolutePath}, size: ${bytes.size} bytes (${bytes.size / 1024}KB)"
                )
                destFile.absolutePath
            } catch (e: Exception) {
                Log.w("StickerFileStorage", "Failed to save tray icon from $sourcePath", e)
                null
            } finally {
                bitmap.recycle()
            }
        }

    actual suspend fun saveTrayImage(sourcePath: String, fileName: String): String =
        trySaveTrayImage(sourcePath, fileName)
            ?: throw IllegalStateException(
                "Tray icon could not be compressed under ${TRAY_MAX_SIZE_KB}KB for WhatsApp: $sourcePath"
            )

    actual suspend fun saveStickerImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                val scaledBitmap = resizeAndFitTo512(bitmap)

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

                Log.d("StickerFileStorage",
                    "Sticker saved: ${destFile.absolutePath}, size: ${bytes.size} bytes (${bytes.size / 1024}KB)")

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
            composeDecorationsOntoBitmap(composedBitmap, decorations)

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

    actual suspend fun extractVideoFrameToFile(
        videoPath: String,
        atMs: Long,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        val source = File(videoPath)
        if (!source.exists()) return@withContext null
        if (source.isGif()) {
            return@withContext extractGifFrameToFile(source, atMs, fileName)
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val rawFrame = retriever.getFrameAtTime(
                atMs.coerceAtLeast(0L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return@withContext null
            // Downscale to a preview-friendly size so we can keep dozens of decoded
            // frames in memory for smooth playback without OOMing on 1080p+ sources.
            // The output is consumed strictly by the trim/crop preview path; the
            // final sticker uses the dedicated decodeVideoFrames pipeline.
            val frame = downscaleForPreview(rawFrame, PREVIEW_FRAME_MAX_DIM)
            val cacheDir = File(context.cacheDir, "video_previews").apply { mkdirs() }
            val outFile = File(cacheDir, fileName)
            FileOutputStream(outFile).use { out ->
                frame.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // Avoid double-recycle: when downscaleForPreview is a no-op it returns
            // the original bitmap, so we only need to free it once.
            if (frame !== rawFrame) {
                frame.recycle()
                rawFrame.recycle()
            } else {
                rawFrame.recycle()
            }
            outFile.absolutePath
        } catch (e: Exception) {
            // The preview screens surface a failure with a retry when nothing can be extracted;
            // swallowing the reason here made that impossible to diagnose from a bug report.
            Log.w(
                "StickerFileStorage",
                "extractVideoFrameToFile failed for $videoPath at ${atMs}ms",
                e
            )
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    /**
     * Scales [source] so the longest edge is at most [maxDim] pixels. Returns the
     * original bitmap when it already fits (caller is responsible for recycling
     * intermediates).
     */
    private fun downscaleForPreview(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = maxOf(w, h)
        if (longest <= maxDim) return source
        val scale = maxDim.toFloat() / longest.toFloat()
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    actual suspend fun getVideoDurationMs(videoPath: String): Long = withContext(Dispatchers.IO) {
        val file = File(videoPath)
        if (!file.exists()) return@withContext -1L
        if (file.isGif()) {
            return@withContext getGifDurationMs(file)
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: -1L
        } catch (e: Exception) {
            // A duration we cannot read silently becomes the 10 s default upstream, so log why.
            Log.w("StickerFileStorage", "getVideoDurationMs failed for $videoPath", e)
            -1L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    actual suspend fun decodeVideoFrames(
        videoPath: String,
        spec: AnimatedStickerSpec,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<DecodedFrame> {
        val sourceFile = File(videoPath)
        if (!sourceFile.exists()) {
            throw IllegalArgumentException("Media file does not exist: $videoPath")
        }
        if (sourceFile.isGif()) {
            return decodeGifFrames(sourceFile, spec, onProgress)
        }
        val mediaItem = MediaItem.fromUri(Uri.fromFile(sourceFile))
        val frameCount = spec.frameCount
        val frameDurationMs = spec.frameDurationMs
        val totalSpan = (spec.trimEndMs - spec.trimStartMs).coerceAtLeast(1L)

        onProgress(0, frameCount)

        // FrameExtractor must be created and accessed from a single application thread.
        // We use Dispatchers.Main.immediate so the underlying ExoPlayer attaches to the main
        // looper; getFrame() returns a future and decoding happens off-thread.
        return withContext(Dispatchers.Main.immediate) {
            val extractor = FrameExtractor.Builder(context, mediaItem).build()
            try {
                val out = ArrayList<DecodedFrame>(frameCount)
                for (i in 0 until frameCount) {
                    val positionMs = if (frameCount == 1) {
                        spec.trimStartMs
                    } else {
                        spec.trimStartMs + i.toLong() * totalSpan / (frameCount - 1).coerceAtLeast(1)
                    }
                    val frame = extractor.getFrame(positionMs).await()
                    val cropTransform = spec.cropTransform
                    val frameBytes = withContext(Dispatchers.IO) {
                        val processed = if (cropTransform != null) {
                            applyCropTransformToBitmap(frame.bitmap, cropTransform)
                        } else {
                            resizeAndCenterCropTo512(frame.bitmap)
                        }
                        val baos = ByteArrayOutputStream()
                        processed.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        if (processed != frame.bitmap) processed.recycle()
                        baos.toByteArray()
                    }
                    out.add(DecodedFrame(frameBytes, frameDurationMs))
                    onProgress(i + 1, frameCount)
                }
                out
            } finally {
                extractor.close()
            }
        }
    }

    actual suspend fun saveAnimatedStickerImage(
        frames: List<DecodedFrame>,
        fileName: String,
        baseDecorations: List<StickerDecoration>,
        frameDecorations: Map<Int, List<StickerDecoration>>,
        onProgress: (current: Int, total: Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (frames.isEmpty()) {
            throw IllegalArgumentException("No frames to encode")
        }

        // Total work units = (compose every frame) + (encoding pass).
        // We reserve the final 25% for encoding so the bar reaches 100% only
        // when the WebP is on disk; before that the user sees the compose phase.
        val composeUnits = frames.size
        val totalUnits = composeUnits + ENCODE_UNITS

        onProgress(0, totalUnits)

        val composed = frames.mapIndexed { idx, frame ->
            val decoded = BitmapFactory.decodeByteArray(frame.bytes, 0, frame.bytes.size)
                ?: throw IllegalStateException("Cannot decode frame $idx")
            val mutable = if (decoded.config == Bitmap.Config.ARGB_8888 && decoded.isMutable) {
                decoded
            } else {
                val converted = decoded.copy(Bitmap.Config.ARGB_8888, true)
                decoded.recycle()
                converted
            }
            val combined = baseDecorations + (frameDecorations[idx] ?: emptyList())
            if (combined.isNotEmpty()) {
                composeDecorationsOntoBitmap(mutable, combined)
            }
            onProgress(idx + 1, totalUnits)
            mutable
        }

        val destFile = File(stickersDir, fileName)
        val durations = frames.map {
            it.durationMs.coerceAtLeast(StickerPack.MIN_FRAME_DURATION_MS)
        }
        val maxBytes = StickerPack.MAX_ANIMATED_STICKER_FILE_SIZE.toLong()

        try {
            val attempts = listOf<Pair<List<Bitmap>, List<Long>>>(
                composed to durations
            ) + buildHalvedAttempts(composed, durations) + buildTrimmedAttempts(composed, durations)

            // Estimate the worst case so we can advance the encode portion smoothly.
            val totalEncodeAttempts = (attempts.size * QUALITY_STOPS.size).coerceAtLeast(1)
            var attemptIdx = 0
            for (attempt in attempts) {
                for (quality in QUALITY_STOPS) {
                    attemptIdx++
                    val encodeFraction = attemptIdx.toFloat() / totalEncodeAttempts
                    val encodedSoFar = (encodeFraction * ENCODE_UNITS).toInt().coerceAtMost(ENCODE_UNITS - 1)
                    onProgress(composeUnits + encodedSoFar, totalUnits)

                    encodeAnimatedWebp(
                        bitmaps = attempt.first,
                        durations = attempt.second,
                        destFile = destFile,
                        quality = quality
                    )
                    val size = destFile.length()
                    Log.d(
                        "StickerFileStorage",
                        "Animated encode attempt: frames=${attempt.first.size}, q=$quality, size=${size / 1024}KB"
                    )
                    // A sticker that only breaks at "Add to WhatsApp" time is expensive to
                    // discover late, so the output is checked before it leaves the loop.
                    verifyAnimatedWebP(destFile)
                    if (size in 1..maxBytes) {
                        onProgress(totalUnits, totalUnits)
                        return@withContext destFile.absolutePath
                    }
                }
            }
            throw IllegalStateException(
                "Cannot fit animated sticker under ${maxBytes / 1024}KB. Try a shorter trim or lower fps."
            )
        } finally {
            composed.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    actual suspend fun encodeSingleFrameAnimatedWebP(
        sourcePath: String,
        fileName: String,
        decorations: List<StickerDecoration>
    ): String = withContext(Dispatchers.IO) {
        val source = BitmapFactory.decodeFile(sourcePath)
            ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")
        try {
            val cropped = resizeAndCenterCropTo512(source)
            val scaled = if (cropped.config == Bitmap.Config.ARGB_8888 && cropped.isMutable) {
                cropped
            } else {
                val converted = cropped.copy(Bitmap.Config.ARGB_8888, true)
                if (cropped !== source) cropped.recycle()
                converted
            }
            if (decorations.isNotEmpty()) {
                composeDecorationsOntoBitmap(scaled, decorations)
            }
            // WhatsApp requires every sticker in an animated pack to report more than one
            // frame. The encoder mirrors the frame it encoded, so the sticker animates on a
            // single image without a second compression pass.
            val destFile = File(stickersDir, fileName)
            val maxBytes = StickerPack.MAX_ANIMATED_STICKER_FILE_SIZE.toLong()
            val durations = listOf(SINGLE_FRAME_ANIMATED_DURATION_MS)
            val bitmaps = listOf(scaled)

            try {
                for (quality in QUALITY_STOPS) {
                    encodeAnimatedWebp(
                        bitmaps = bitmaps,
                        durations = durations,
                        destFile = destFile,
                        quality = quality
                    )
                    verifyAnimatedWebP(destFile)
                    if (destFile.length() in 1..maxBytes) {
                        return@withContext destFile.absolutePath
                    }
                }
                throw IllegalStateException(
                    "Cannot fit re-encoded static sticker under ${maxBytes / 1024}KB"
                )
            } finally {
                if (!scaled.isRecycled) scaled.recycle()
            }
        } finally {
            if (!source.isRecycled) source.recycle()
        }
    }

    /**
     * Assert that the file on disk is an animated WebP with at least [MIN_ANIMATED_FRAMES]
     * frames. [WebpContainer] writes exactly the frames it is given, so this can only trip if
     * the write itself went wrong; it stays because the failure it guards — a sticker that only
     * turns out to be broken at "Add to WhatsApp" time — is expensive to discover late.
     */
    private fun verifyAnimatedWebP(file: File) {
        val frames = WebpContainer.frameCount(file.readBytes())
        if (frames < MIN_ANIMATED_FRAMES) {
            file.delete()
            throw IllegalStateException(
                "Encoded WebP only has $frames frame(s); WhatsApp requires animated stickers " +
                    "with at least $MIN_ANIMATED_FRAMES frames."
            )
        }
    }

    private fun buildHalvedAttempts(
        bitmaps: List<Bitmap>,
        durations: List<Long>
    ): List<Pair<List<Bitmap>, List<Long>>> {
        if (bitmaps.size < 6) return emptyList()
        val halved = bitmaps.filterIndexed { i, _ -> i % 2 == 0 }
        val halvedDur = durations
            .filterIndexed { i, _ -> i % 2 == 0 }
            .map { it * 2 }
        return listOf(halved to halvedDur)
    }

    private fun buildTrimmedAttempts(
        bitmaps: List<Bitmap>,
        durations: List<Long>
    ): List<Pair<List<Bitmap>, List<Long>>> {
        if (bitmaps.size < 4) return emptyList()
        val keep = (bitmaps.size * 2) / 3
        return listOf(
            bitmaps.take(keep) to durations.take(keep)
        )
    }

    /**
     * Encode [bitmaps] as an animated WebP that satisfies WhatsApp's frame rules.
     *
     * The frames are compressed with the platform encoder — `Bitmap.compress` produces a still
     * WebP — and [WebpContainer] wraps them into the animation container. Doing the wrapping
     * here rather than in libwebp means a frame can never be switched out or dropped: the file
     * contains the frames we pass, in order.
     *
     * Only the part of a frame that changed since the previous one is encoded, and the rest of
     * the canvas keeps showing the previous frame. Without that, a 20-frame sticker stores 20
     * full 512x512 pictures and blows past WhatsApp's 500 KB limit.
     *
     * A single-frame source is mirrored rather than rejected, because WhatsApp refuses an
     * animated pack whose sticker reports `frameCount <= 1`.
     */
    private fun encodeAnimatedWebp(
        bitmaps: List<Bitmap>,
        durations: List<Long>,
        destFile: File,
        quality: Float
    ) {
        require(bitmaps.isNotEmpty()) { "Cannot encode animated WebP with zero frames" }
        if (destFile.exists()) destFile.delete()

        val frames = ArrayList<WebpContainer.Frame>(bitmaps.size)
        var previousPixels: IntArray? = null
        bitmaps.forEachIndexed { index, bitmap ->
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val rect = changedRect(previousPixels, pixels, bitmap.width, bitmap.height)
            previousPixels = pixels

            val region = bitmap.cropTo(rect)
            try {
                frames += WebpContainer.Frame(
                    stillImage = region.compressToWebP(quality),
                    durationMs = durations[index]
                        .coerceAtLeast(StickerPack.MIN_FRAME_DURATION_MS)
                        .toInt(),
                    rect = rect
                )
            } finally {
                if (region !== bitmap) region.recycle()
            }
        }

        val animated = if (frames.size < MIN_ANIMATED_FRAMES) {
            val last = frames.last()
            frames + WebpContainer.Frame(last.stillImage, last.durationMs, last.rect)
        } else {
            frames
        }

        destFile.writeBytes(
            WebpContainer.animate(
                canvasWidth = StickerPack.STICKER_SIZE,
                canvasHeight = StickerPack.STICKER_SIZE,
                frames = animated,
                loopCount = 0
            )
        )
    }

    /**
     * The smallest rectangle that differs between [previous] and [current], with its left edge
     * and top edge moved to an even coordinate (the container stores those divided by two).
     * The first frame, and a frame identical to the previous one, cover the whole canvas.
     */
    private fun changedRect(
        previous: IntArray?,
        current: IntArray,
        width: Int,
        height: Int
    ): WebpContainer.Rect {
        val full = WebpContainer.Rect(0, 0, width, height)
        if (previous == null || previous.size != current.size) return full

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (current[row + x] != previous[row + x]) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) return full

        val left = minX and 1.inv()
        val top = minY and 1.inv()
        // A one-pixel-wide frame is legal, but keeping the rectangle at least two pixels wide
        // avoids a degenerate ANMF for a single changed column.
        val rectWidth = ((maxX + 1 - left).coerceAtLeast(2)).coerceAtMost(width - left)
        val rectHeight = ((maxY + 1 - top).coerceAtLeast(2)).coerceAtMost(height - top)
        return WebpContainer.Rect(left, top, rectWidth, rectHeight)
    }

    /**
     * Copy [rect] out of this bitmap. The copy owns its pixels, so recycling it cannot take the
     * frame it came from down with it — which is what a shared subset would do.
     */
    private fun Bitmap.cropTo(rect: WebpContainer.Rect): Bitmap {
        if (rect.left == 0 && rect.top == 0 && rect.width == width && rect.height == height) {
            return this
        }
        val cropped = Bitmap.createBitmap(rect.width, rect.height, Bitmap.Config.ARGB_8888)
        Canvas(cropped).drawBitmap(this, -rect.left.toFloat(), -rect.top.toFloat(), null)
        return cropped
    }

    /**
     * Compress one frame to a still WebP with the platform encoder.
     *
     * `WEBP_LOSSY` is the lossy constant from API 30 on; before that the same encoder is
     * reached through the deprecated `WEBP`, which is what `convertToWebP` above already does.
     * Quality is capped at 99 on the older API: from API 29 on, `WEBP` at quality 100 switches
     * to the *lossless* encoder, which produces a `VP8L` frame instead of a lossy `VP8` one.
     */
    private fun Bitmap.compressToWebP(quality: Float): ByteArray {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        val maxQuality = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) 99 else 100
        val out = ByteArrayOutputStream()
        check(compress(format, quality.toInt().coerceIn(1, maxQuality), out)) {
            "The platform WebP encoder refused a ${width}x$height frame"
        }
        return out.toByteArray()
    }

    private fun File.isGif(): Boolean = extension.equals("gif", ignoreCase = true)

    /**
     * Decode animated GIF using [Movie] (API 24+). Timeline matches in-app trim/crop/FPS like video.
     */
    @Suppress("DEPRECATION")
    private suspend fun decodeGifFrames(
        file: File,
        spec: AnimatedStickerSpec,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<DecodedFrame> =
        withContext(Dispatchers.IO) {
            val movie = FileInputStream(file).use { Movie.decodeStream(it) }
                ?: throw IllegalArgumentException("Cannot decode GIF: ${file.absolutePath}")
            val gifDuration = movie.duration().coerceAtLeast(1)
            val gifDurMs = gifDuration.toLong().coerceAtLeast(1L)
            val frameCount = spec.frameCount
            val frameDurationMs = spec.frameDurationMs
            val trimStart = spec.trimStartMs.coerceIn(0L, gifDurMs - 1)
            val trimEnd = spec.trimEndMs.coerceIn(trimStart + 1, gifDurMs)
            val totalSpan = (trimEnd - trimStart).coerceAtLeast(1L)
            val cropTransform = spec.cropTransform
            val out = ArrayList<DecodedFrame>(frameCount)
            onProgress(0, frameCount)
            for (i in 0 until frameCount) {
                val positionMsLong = if (frameCount == 1) {
                    trimStart
                } else {
                    trimStart + i.toLong() * totalSpan / (frameCount - 1).coerceAtLeast(1)
                }
                val positionMs = positionMsLong.toInt().coerceIn(0, gifDuration - 1)
                movie.setTime(positionMs)
                val w = movie.width().coerceAtLeast(1)
                val h = movie.height().coerceAtLeast(1)
                val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(raw).also { canvas -> movie.draw(canvas, 0f, 0f) }
                val processed = if (cropTransform != null) {
                    applyCropTransformToBitmap(raw, cropTransform)
                } else {
                    resizeAndCenterCropTo512(raw)
                }
                if (processed != raw) raw.recycle()
                val baos = ByteArrayOutputStream()
                processed.compress(Bitmap.CompressFormat.PNG, 100, baos)
                processed.recycle()
                out.add(DecodedFrame(baos.toByteArray(), frameDurationMs))
                onProgress(i + 1, frameCount)
            }
            out
        }

    @Suppress("DEPRECATION")
    private fun getGifDurationMs(file: File): Long {
        return try {
            FileInputStream(file).use { stream ->
                val movie = Movie.decodeStream(stream)
                movie?.duration()?.toLong()?.takeIf { it > 0 } ?: -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }

    @Suppress("DEPRECATION")
    private fun extractGifFrameToFile(file: File, atMs: Long, fileName: String): String? {
        return try {
            val movie = FileInputStream(file).use { Movie.decodeStream(it) } ?: return null
            val dur = movie.duration().coerceAtLeast(1)
            val t = atMs.toInt().coerceIn(0, dur - 1)
            movie.setTime(t)
            val w = movie.width().coerceAtLeast(1)
            val h = movie.height().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bmp).also { movie.draw(it, 0f, 0f) }
            val cacheDir = File(context.cacheDir, "video_previews").apply { mkdirs() }
            val outFile = File(cacheDir, fileName)
            FileOutputStream(outFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun resizeAndCenterCropTo512(source: Bitmap): Bitmap =
        resizeAndCenterCropToSize(source, StickerPack.STICKER_SIZE)

    /**
     * Aspect-fit into a 512×512 canvas (letterboxing), matching [ContentScale.Fit] in the editor
     * preview and [applyCropTransformation] math so decoration coordinates stay aligned.
     */
    private fun resizeAndFitTo512(source: Bitmap): Bitmap =
        resizeAndFitToSize(source, StickerPack.STICKER_SIZE)

    private fun resizeAndFitToSize(source: Bitmap, target: Int): Bitmap {
        if (source.width == target && source.height == target) {
            return if (source.config == Bitmap.Config.ARGB_8888) {
                source
            } else {
                source.copy(Bitmap.Config.ARGB_8888, false)
            }
        }

        val output = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val maxDim = maxOf(source.width, source.height).toFloat().coerceAtLeast(1f)
        val fitScale = target / maxDim
        val scaledW = source.width * fitScale
        val scaledH = source.height * fitScale
        val left = (target - scaledW) / 2f
        val top = (target - scaledH) / 2f

        val matrix = Matrix().apply {
            postScale(fitScale, fitScale)
            postTranslate(left, top)
        }
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    private fun resizeAndCenterCropToSize(source: Bitmap, target: Int): Bitmap {
        if (source.width == target && source.height == target) return source

        val scale = maxOf(
            target.toFloat() / source.width.toFloat(),
            target.toFloat() / source.height.toFloat()
        )
        val scaledW = (source.width * scale).toInt().coerceAtLeast(target)
        val scaledH = (source.height * scale).toInt().coerceAtLeast(target)
        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)

        val xOffset = ((scaled.width - target) / 2).coerceAtLeast(0)
        val yOffset = ((scaled.height - target) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, xOffset, yOffset, target, target)
        if (scaled != source && scaled != cropped) scaled.recycle()
        return cropped
    }

    /**
     * Tries 96×96 down to WhatsApp's 24px minimum, lowering PNG quality each step until ≤50 KB.
     */
    private fun compressTrayBitmapForWhatsApp(source: Bitmap): ByteArray? {
        val maxBytes = TRAY_MAX_SIZE_BYTES
        val targetSizes = listOf(
            StickerPack.TRAY_ICON_SIZE,
            80, 72, 64, 56, 48, 40, 32, 24
        ).distinct().sortedDescending()
        for (targetSize in targetSizes) {
            val cropped = resizeAndCenterCropToSize(source, targetSize)
            val bytes = compressTrayPngLoop(cropped, maxBytes)
            if (cropped !== source) cropped.recycle()
            if (bytes != null) return bytes
        }
        return null
    }

    private fun compressTrayPngLoop(bitmap: Bitmap, maxBytes: Int): ByteArray? {
        var quality = 100
        while (quality >= 1) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
            val bytes = stream.toByteArray()
            if (bytes.size <= maxBytes) return bytes
            quality -= if (quality > 20) 10 else if (quality > 5) 5 else 1
        }
        return null
    }

    /**
     * Apply [transform] to [source] and produce a 512×512 bitmap. We mirror the math used by
     * `VideoCropScreen` so the saved frame matches what the user previewed:
     *  - `scale` multiplies the aspect-fit scale derived from source dimensions (no stretch).
     *  - `offset*Norm` is normalized to the 512×512 output box.
     *  - `flipHorizontal/Vertical` and `rotation` are applied around the source center.
     */
    private fun applyCropTransformToBitmap(source: Bitmap, transform: CropTransform): Bitmap {
        val target = StickerPack.STICKER_SIZE
        val output = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val maxDim = maxOf(source.width, source.height).toFloat().coerceAtLeast(1f)
        val fitScale = target.toFloat() / maxDim
        val finalScale = transform.scale.coerceIn(CropTransform.MIN_SCALE, CropTransform.MAX_SCALE) * fitScale

        val matrix = Matrix().apply {
            postTranslate(-source.width / 2f, -source.height / 2f)
            postScale(finalScale, finalScale)
            if (transform.flipHorizontal) postScale(-1f, 1f)
            if (transform.flipVertical) postScale(1f, -1f)
            postRotate(transform.rotation)
            postTranslate(
                target / 2f + transform.offsetXNorm * target,
                target / 2f + transform.offsetYNorm * target
            )
        }
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    private suspend fun composeDecorationsOntoBitmap(
        target: Bitmap,
        decorations: List<StickerDecoration>
    ) {
        if (decorations.isEmpty()) return
        val overlay = withContext(Dispatchers.Main) {
            AndroidDecorationBitmapRenderer.render(
                context = context,
                width = target.width,
                height = target.height,
                decorations = decorations
            )
        }
        Canvas(target).drawBitmap(overlay, 0f, 0f, null)
        overlay.recycle()
    }

    companion object {
        private const val TRAY_MAX_SIZE_KB = 50
        private const val TRAY_MAX_SIZE_BYTES = TRAY_MAX_SIZE_KB * 1024

        private val QUALITY_STOPS = listOf(80f, 65f, 50f, 35f, 22f)

        /**
         * Number of progress units the encode phase reserves. Picked so the bar
         * spends roughly 25-30% of its travel on the encode pass, which matches
         * how long encoding actually takes vs. compositing decorations.
         */
        private const val ENCODE_UNITS = 8

        /**
         * Per-frame duration used when re-encoding a static sticker as a 2-frame
         * "animated" WebP for animated packs. We use 1000ms so the animation feels
         * like a still image to the user and stays well under the 10s total cap.
         */
        private const val SINGLE_FRAME_ANIMATED_DURATION_MS = 1000L

        /**
         * WhatsApp's `StickerPackValidator` rejects any sticker inside an animated pack
         * with `webPImage.getFrameCount() <= 1`. The encoder mirrors a single-frame source
         * up to this count.
         */
        private const val MIN_ANIMATED_FRAMES = 2

        /**
         * Largest edge (px) we keep for video preview frames extracted by
         * [extractVideoFrameToFile]. Trim/Crop screens can hold up to ~40 of these
         * decoded in memory at once for smooth playback, so capping the long edge
         * here lets us store many frames cheaply (~150 KB each) without OOM, while
         * still looking sharp inside the square preview frame.
         */
        private const val PREVIEW_FRAME_MAX_DIM = 384
    }
}
