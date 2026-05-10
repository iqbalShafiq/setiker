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

        /** How many thumbnails we extract for the preview strip. Cheap to keep at ~12. */
        const val PREVIEW_THUMBNAIL_COUNT = 12
    }
}
