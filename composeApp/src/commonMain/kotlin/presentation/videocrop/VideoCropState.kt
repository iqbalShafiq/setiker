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
        /** Number of preview samples we extract for the crop screen playback. */
        const val PREVIEW_FRAME_COUNT = 8
    }
}
