package presentation.videocrop

import domain.model.AnimatedStickerSpec
import domain.model.CropTransform

/**
 * One sample frame extracted at a uniform timestamp inside the trim. We keep a
 * small set of these so the user can press play and see the crop applied to the
 * actual moving footage instead of just a static still.
 */
data class VideoCropPreviewFrame(
    val timestampMs: Long,
    val filePath: String
)

data class VideoCropState(
    val videoPath: String = "",
    val previewFrames: List<VideoCropPreviewFrame> = emptyList(),
    val currentPreviewIndex: Int = 0,
    val previewWidth: Int = 0,
    val previewHeight: Int = 0,
    val spec: AnimatedStickerSpec? = null,
    val packId: String = "",
    val transform: CropTransform = CropTransform(),
    val isLoadingPreview: Boolean = true,
    val isPlaying: Boolean = false,
    val isApplying: Boolean = false,
    val applyProgress: Float = 0f,
    val applyProgressLabel: String? = null,
    val errorMessage: String? = null
) {
    /** The image we currently render in the preview frame. */
    val currentPreviewPath: String?
        get() = previewFrames.getOrNull(currentPreviewIndex.coerceIn(0, (previewFrames.size - 1).coerceAtLeast(0)))?.filePath

    val canPlay: Boolean get() = previewFrames.size > 1

    companion object {
        /**
         * Preview samples extracted inside the trim range. Bumped from 8 to 32
         * so playback reads as video rather than a slideshow — 8 frames over a
         * 5 s trim is ~1.6 fps which felt like the speed slider did nothing.
         * Each frame is downscaled to 384 px by the storage layer so the full
         * set stays small in RAM (~5 MB).
         */
        const val PREVIEW_FRAME_COUNT = 32
    }
}
