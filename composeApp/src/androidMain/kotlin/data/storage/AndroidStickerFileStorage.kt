package data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.concurrent.futures.await
import androidx.media3.common.MediaItem
import androidx.media3.transformer.ExperimentalFrameExtractor
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoderOptions
import com.aureusapps.android.webpandroid.encoder.WebPConfig
import com.aureusapps.android.webpandroid.encoder.WebPMuxAnimParams
import com.aureusapps.android.webpandroid.encoder.WebPPreset
import domain.model.AnimatedStickerSpec
import domain.model.CropTransform
import domain.model.DecodedFrame
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.StickerDecoration
import domain.model.StickerPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
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
                android.util.Log.d(
                    "StickerFileStorage",
                    "Tray icon saved: ${destFile.absolutePath}, size: ${bytes.size} bytes (${bytes.size / 1024}KB)"
                )
                destFile.absolutePath
            } catch (e: Exception) {
                android.util.Log.w("StickerFileStorage", "Failed to save tray icon from $sourcePath", e)
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

                android.util.Log.d("StickerFileStorage",
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
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val rawFrame = retriever.getFrameAtTime(
                atMs.coerceAtLeast(0L) * 1000L,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
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
        } catch (_: Exception) {
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
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: -1L
        } catch (_: Exception) {
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

        // ExperimentalFrameExtractor must be created and accessed from a single application
        // thread. We use Dispatchers.Main.immediate so the underlying ExoPlayer attaches to
        // the main looper; getFrame() returns a future and decoding happens off-thread.
        return withContext(Dispatchers.Main.immediate) {
            val extractor = ExperimentalFrameExtractor(
                context,
                ExperimentalFrameExtractor.Configuration.Builder().build()
            )
            try {
                extractor.setMediaItem(mediaItem, /* effects = */ emptyList())
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
                extractor.release()
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
                        quality = quality,
                        minimizeSize = true
                    )
                    val size = destFile.length()
                    android.util.Log.d(
                        "StickerFileStorage",
                        "Animated encode attempt: frames=${attempt.first.size}, q=$quality, size=${size / 1024}KB"
                    )
                    // Confirm libwebp didn't silently collapse our frames. If it did, the
                    // resulting WebP would fail WhatsApp's `frameCount > 1` check and the
                    // user would only learn at "Add to WhatsApp" time.
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
            // We encode two frames so the output WebP has the ANIM chunk and
            // `webPImage.getFrameCount() > 1`, which WhatsApp's StickerPackValidator
            // requires for every sticker inside an animated pack.
            //
            // The second frame is *almost* identical to the first but with a single
            // pixel's alpha nudged by one unit. Without this nudge libwebp's
            // anim_encode skips the frame entirely (see `IsEmptyRect` /
            // `PixelsAreSimilar` in `mux/anim_encode.c` — alpha must match exactly
            // for two pixels to be considered "similar"), leaving us with a 1-frame
            // WebP that fails validation. `minimizeSize = false` alone is NOT
            // sufficient because that flag does not gate the empty-rect skip.
            val twin = makeAlphaNudgedTwin(scaled)
            val destFile = File(stickersDir, fileName)
            val maxBytes = StickerPack.MAX_ANIMATED_STICKER_FILE_SIZE.toLong()
            val durations = listOf(SINGLE_FRAME_ANIMATED_DURATION_MS, SINGLE_FRAME_ANIMATED_DURATION_MS)
            val bitmaps = listOf(scaled, twin)

            try {
                for (quality in QUALITY_STOPS) {
                    encodeAnimatedWebp(
                        bitmaps = bitmaps,
                        durations = durations,
                        destFile = destFile,
                        quality = quality,
                        minimizeSize = false
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
                if (!twin.isRecycled) twin.recycle()
                if (!scaled.isRecycled) scaled.recycle()
            }
        } finally {
            if (!source.isRecycled) source.recycle()
        }
    }

    /**
     * Produce a copy of [source] with the alpha of pixel `(0, 0)` shifted by one. The
     * pixel is in the top-left corner — typically transparent for a sticker — and a
     * 1/255 alpha delta is below the perceptual threshold, so the output is visually
     * identical to the input.
     *
     * This is what stops libwebp from skipping our second frame: its
     * `MinimizeChangeRectangle` shrinks the per-frame change rectangle to zero when
     * every pixel matches "closely enough", but `PixelsAreSimilar` requires the alpha
     * channel to match exactly. A 1-unit alpha tweak therefore guarantees the change
     * rectangle has a non-zero area, which forces the encoder to actually emit the
     * second ANMF chunk.
     */
    private fun makeAlphaNudgedTwin(source: Bitmap): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixel = copy.getPixel(0, 0)
        val alpha = (pixel ushr 24) and 0xFF
        val nudged = if (alpha == 0) 1 else alpha - 1
        copy.setPixel(0, 0, (nudged shl 24) or (pixel and 0x00FFFFFF))
        return copy
    }

    /**
     * Walk the RIFF chunk structure of [file] and assert it is an animated WebP with
     * at least two ANMF (frame) chunks. We intentionally do this inline instead of
     * decoding the file through `BitmapFactory`, because we want to catch the silent
     * "encoder dropped a frame" regression before the file leaves the encoder loop.
     *
     * If verification fails we delete the offending file so the outer retry loop can
     * try a different quality without seeing a stale on-disk artefact.
     */
    private fun verifyAnimatedWebP(file: File) {
        val bytes = file.readBytes()
        val frames = countAnmfChunks(bytes)
        if (frames < MIN_ANIMATED_FRAMES) {
            file.delete()
            throw IllegalStateException(
                "Encoded WebP only has $frames frame(s); WhatsApp requires animated stickers " +
                    "with at least $MIN_ANIMATED_FRAMES frames. This usually means libwebp " +
                    "deduplicated near-identical frames."
            )
        }
    }

    private fun countAnmfChunks(bytes: ByteArray): Int {
        if (bytes.size < HEADER_PREFIX_LEN) return 0
        // RIFF header: "RIFF" <size:4> "WEBP" then chunks.
        if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
            bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte() ||
            bytes[8] != 'W'.code.toByte() || bytes[9] != 'E'.code.toByte() ||
            bytes[10] != 'B'.code.toByte() || bytes[11] != 'P'.code.toByte()
        ) {
            return 0
        }
        var offset = HEADER_PREFIX_LEN
        var count = 0
        while (offset + CHUNK_HEADER_LEN <= bytes.size) {
            val fourcc = String(bytes, offset, 4, Charsets.US_ASCII)
            // Chunk size is an unsigned 32-bit LE int but we never expect > 500KB.
            val size = (bytes[offset + 4].toInt() and 0xFF) or
                ((bytes[offset + 5].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 6].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 7].toInt() and 0xFF) shl 24)
            if (fourcc == "ANMF") count++
            // RIFF chunks are padded to even byte boundaries.
            val padded = size + (size and 1)
            offset += CHUNK_HEADER_LEN + padded
        }
        return count
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
     * Encode [bitmaps] as an animated WebP that always satisfies WhatsApp's
     * `webPImage.getFrameCount() > 1` rule (see WhatsApp/stickers `StickerPackValidator`).
     *
     * Two failure modes we defend against here:
     *  - Source has < 2 frames (e.g. a 50ms GIF or a static-as-animated re-encode): we
     *    duplicate the last frame so the encoder always sees ≥ 2 frames.
     *  - libwebp with `minimize_size = 1` may collapse runs of identical consecutive frames
     *    into a single frame. When the caller is producing intentionally identical frames
     *    (static sticker → 2-frame anim path), it must pass `minimizeSize = false` so the
     *    second frame survives.
     */
    private fun encodeAnimatedWebp(
        bitmaps: List<Bitmap>,
        durations: List<Long>,
        destFile: File,
        quality: Float,
        minimizeSize: Boolean
    ) {
        require(bitmaps.isNotEmpty()) { "Cannot encode animated WebP with zero frames" }
        if (destFile.exists()) destFile.delete()

        // Pad to at least 2 frames. WhatsApp rejects animated packs whose stickers report
        // frameCount <= 1, so a 1-frame source would silently fail at validation time.
        // The padded frame uses the alpha-nudge trick so libwebp's anim_encode doesn't skip
        // it back out (see `makeAlphaNudgedTwin` for why a pixel-identical duplicate is
        // collapsed regardless of the `minimize_size` flag).
        val padded = bitmaps.size < MIN_ANIMATED_FRAMES
        val safeBitmaps: List<Bitmap>
        val safeDurations: List<Long>
        val paddedTwin: Bitmap? = if (padded) makeAlphaNudgedTwin(bitmaps.last()) else null
        if (padded && paddedTwin != null) {
            safeBitmaps = bitmaps + paddedTwin
            safeDurations = durations + durations.last().coerceAtLeast(StickerPack.MIN_FRAME_DURATION_MS)
        } else {
            safeBitmaps = bitmaps
            safeDurations = durations
        }

        val encoder = WebPAnimEncoder(
            context = context,
            width = StickerPack.STICKER_SIZE,
            height = StickerPack.STICKER_SIZE,
            options = WebPAnimEncoderOptions(
                minimizeSize = minimizeSize,
                animParams = WebPMuxAnimParams(
                    backgroundColor = 0,
                    loopCount = 0
                )
            )
        )
        try {
            encoder.configure(
                config = WebPConfig(
                    lossless = WebPConfig.COMPRESSION_LOSSY,
                    quality = quality
                ),
                preset = WebPPreset.WEBP_PRESET_PICTURE
            )
            var timestamp = 0L
            safeBitmaps.forEachIndexed { i, bmp ->
                encoder.addFrame(timestamp, bmp)
                timestamp += safeDurations[i]
            }
            encoder.assemble(timestamp, Uri.fromFile(destFile))
        } finally {
            encoder.release()
            paddedTwin?.takeIf { !it.isRecycled }?.recycle()
        }
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
        val overlay = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
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
         * with `webPImage.getFrameCount() <= 1`. We pad single-frame sources up to this
         * count before handing them to the encoder.
         */
        private const val MIN_ANIMATED_FRAMES = 2

        /** Length of the WebP RIFF prefix: `RIFF <size:4> WEBP`. */
        private const val HEADER_PREFIX_LEN = 12

        /** Length of a single RIFF chunk header: 4-byte FourCC + 4-byte size. */
        private const val CHUNK_HEADER_LEN = 8

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
