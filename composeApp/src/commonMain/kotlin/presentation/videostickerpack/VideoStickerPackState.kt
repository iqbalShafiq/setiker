package presentation.videostickerpack

import domain.model.CandidateGridImage
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.VideoFrameCandidate
import domain.model.VideoStickerCandidateManifestItem

data class VideoStickerPackState(
    val videoPath: String = "",
    val previewFramePath: String? = null,
    val sourceDurationMs: Long = 0L,
    val selectedStartMs: Long = 0L,
    val selectedEndMs: Long = 0L,
    val prompt: String = "",
    val packName: String = "",
    val publisher: String = "",
    val isLoadingVideo: Boolean = false,
    val isProcessing: Boolean = false,
    val processingStep: VideoStickerPackProcessingStep? = null,
    val processingProgress: Float = 0f,
    val candidates: List<VideoFrameCandidate> = emptyList(),
    val candidateGrids: List<CandidateGridImage> = emptyList(),
    val candidateManifest: List<VideoStickerCandidateManifestItem> = emptyList(),
    val generatedPlan: ResolvedVideoStickerPackPlan? = null,
    val errorMessage: String? = null
) {
    val selectedDurationMs: Long get() = selectedEndMs - selectedStartMs

    val canGenerate: Boolean
        get() = videoPath.isNotBlank() && selectedDurationMs in 1L..60_000L && !isProcessing

    val canSave: Boolean
        get() = generatedPlan?.let { it.staticStickers.isNotEmpty() || it.animatedStickers.isNotEmpty() } == true &&
            packName.isNotBlank() && publisher.isNotBlank() && !isProcessing
}

enum class VideoStickerPackProcessingStep {
    FindingFrames,
    BuildingGrids,
    AskingAi,
    PreparingPreview,
    Saving
}
