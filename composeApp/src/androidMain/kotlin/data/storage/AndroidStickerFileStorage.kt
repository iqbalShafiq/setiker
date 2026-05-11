@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.StickerPack
import domain.model.TextDecoration
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

                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    actual suspend fun saveTrayImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, true)

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

                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    actual suspend fun saveStickerImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

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
            val frame = retriever.getFrameAtTime(
                atMs.coerceAtLeast(0L) * 1000L,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return@withContext null
            val cacheDir = File(context.cacheDir, "video_previews").apply { mkdirs() }
            val outFile = File(cacheDir, fileName)
            FileOutputStream(outFile).use { out ->
                frame.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            frame.recycle()
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
        val target = StickerPack.STICKER_SIZE
        try {
            val scaled = if (source.width == target && source.height == target) {
                source.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                Bitmap.createScaledBitmap(source, target, target, true)
                    .copy(Bitmap.Config.ARGB_8888, true)
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

    private fun resizeAndCenterCropTo512(source: Bitmap): Bitmap {
        val target = StickerPack.STICKER_SIZE
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

    private fun composeDecorationsOntoBitmap(
        target: Bitmap,
        decorations: List<StickerDecoration>
    ) {
        val canvas = Canvas(target)
        val minDim = minOf(target.width, target.height).toFloat()
        decorations.forEach { decoration ->
            val centerX = decoration.centerX.coerceIn(0f, 1f) * target.width
            val centerY = decoration.centerY.coerceIn(0f, 1f) * target.height
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
                            bitmapWidth = target.width,
                            bitmapHeight = target.height,
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
    }

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

    companion object {
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
    }
}
