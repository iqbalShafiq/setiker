package data.storage

import domain.model.AnimatedStickerSpec
import domain.model.DecodedFrame
import domain.model.StickerDecoration

expect class StickerFileStorage {
    suspend fun saveImage(sourcePath: String, fileName: String): String
    suspend fun saveBytes(bytes: ByteArray, fileName: String): String
    suspend fun loadImage(fileName: String): ByteArray?
    suspend fun deleteImage(fileName: String): Boolean
    suspend fun getImagePath(fileName: String): String
    suspend fun imageExists(fileName: String): Boolean
    suspend fun convertToWebP(sourcePath: String, outputFileName: String): String
    suspend fun saveTrayImage(sourcePath: String, fileName: String): String
    suspend fun saveStickerImage(sourcePath: String, fileName: String): String
    suspend fun saveStickerImageWithDecorations(
        sourcePath: String,
        fileName: String,
        decorations: List<StickerDecoration>
    ): String

    /**
     * Get the duration of [videoPath] in milliseconds, or -1 if it cannot be probed.
     * Supports regular video files and animated GIF (`.gif`).
     */
    suspend fun getVideoDurationMs(videoPath: String): Long

    /**
     * Extract a single representative frame from [videoPath] at [atMs] and save it as a PNG
     * file at [fileName] inside an internal cache. Returns the absolute path or `null` if
     * extraction fails. Supports video and animated GIF.
     */
    suspend fun extractVideoFrameToFile(videoPath: String, atMs: Long, fileName: String): String?

    /**
     * Decode a video file **or animated GIF** into a list of 512×512 PNG-encoded frames according to [spec].
     *
     * Implementations must:
     * - Sample frames at uniform intervals between [AnimatedStickerSpec.trimStartMs] and [AnimatedStickerSpec.trimEndMs].
     * - Return frames already resized/center-cropped to 512×512 (or user [CropTransform] when set).
     * - Set [DecodedFrame.durationMs] to `1000 / spec.fps` (≥ 8 ms).
     *
     * [onProgress] is invoked as `(current, total)` whenever a frame is fully decoded so the
     * caller can drive a real progress bar. `current == total` means decoding finished.
     */
    suspend fun decodeVideoFrames(
        videoPath: String,
        spec: AnimatedStickerSpec,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<DecodedFrame>

    /**
     * Compose decorations onto each frame and encode as animated WebP that satisfies WhatsApp's
     * 500 KB / 512×512 / 10 s constraints. Implementations must enforce an adaptive size loop.
     *
     * @param baseDecorations decorations applied to every frame (background overlay).
     * @param frameDecorations per-frame additional decorations keyed by frame index.
     * @param onProgress invoked as `(current, total)`. Implementations report progress through
     *   the compose phase (`current == 0..frames.size`) and the encode phase (final `current == total`).
     */
    suspend fun saveAnimatedStickerImage(
        frames: List<DecodedFrame>,
        fileName: String,
        baseDecorations: List<StickerDecoration> = emptyList(),
        frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap(),
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): String

    /**
     * Re-encode a static image as a 1-frame animated WebP so it can live inside a pack that
     * also contains true animated stickers (WhatsApp packs may not mix static and animated
     * WebP files). Implementations must apply [decorations] just like the static save path.
     */
    suspend fun encodeSingleFrameAnimatedWebP(
        sourcePath: String,
        fileName: String,
        decorations: List<StickerDecoration> = emptyList()
    ): String
}
