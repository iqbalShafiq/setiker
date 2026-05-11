package presentation.videotrim

import domain.model.AnimatedStickerSpec
import domain.model.StickerPack

/**
 * One pre-extracted thumbnail used to drive both the static preview and the
 * play/pause animation in [VideoTrimScreen]. Order is left-to-right along the
 * source video timeline.
 */
data class VideoTrimThumbnail(
    val timestampMs: Long,
    val filePath: String
)

data class VideoTrimState(
    val videoPath: String = "",
    val videoDurationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val fps: Int = AnimatedStickerSpec.DEFAULT_FPS,
    val speed: Float = 1f,
    val thumbnails: List<VideoTrimThumbnail> = emptyList(),
    val currentThumbIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val errorMessage: String? = null
) {
    val maxAllowedTrimMs: Long get() = StickerPack.MAX_ANIMATION_DURATION_MS
    val effectiveTrimMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    /** Thumbnails that fall inside the current trim selection (used for playback). */
    val thumbnailsInRange: List<VideoTrimThumbnail>
        get() = thumbnails.filter { it.timestampMs in trimStartMs..trimEndMs }

    /** Path of the thumbnail that should currently be rendered, or null when none ready. */
    val currentPreviewPath: String?
        get() {
            if (thumbnails.isEmpty()) return null
            val pool = thumbnailsInRange.ifEmpty { thumbnails }
            val idx = currentThumbIndex.coerceIn(0, pool.size - 1)
            return pool[idx].filePath
        }

    val canPlay: Boolean get() = thumbnailsInRange.size > 1

    fun toSpec(): AnimatedStickerSpec = AnimatedStickerSpec(
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        fps = fps,
        speed = speed
    )

    companion object {
        val FPS_OPTIONS = listOf(10, 15, 20, 24)
        val SPEED_OPTIONS = listOf(0.5f, 1f, 1.5f, 2f)

        /**
         * Frames we extract inside the current trim window for the play preview.
         * 32 frames over a 4 s trim is ~8 fps preview, which is the lowest count
         * that visually reads as "video" instead of "slideshow" — the previous 12
         * across the whole video meant a small trim could end up with 2-3 in-range
         * frames and felt disconnected from the speed chips. Each frame is
         * downscaled to 384 px by the storage layer so the full set fits in
         * memory and extraction stays under ~3 s for typical sticker sources.
         */
        const val PREVIEW_THUMBNAIL_COUNT = 32
    }
}
