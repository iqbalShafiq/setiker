package presentation.videostickerpack

import domain.model.CandidateGridImage
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.VideoFrameCandidate
import domain.model.VideoStickerCandidateManifestItem

data class VideoStickerPackState(
    val videoPath: String = "",
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
    val selectedStaticStickerKeys: Set<String> = emptySet(),
    val selectedAnimatedStickerKeys: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    val selectedDurationMs: Long get() = selectedEndMs - selectedStartMs

    val selectedStickerCount: Int
        get() = selectedStaticStickerKeys.size + selectedAnimatedStickerKeys.size

    val isSaving: Boolean
        get() = processingStep == VideoStickerPackProcessingStep.Saving && isProcessing

    val isBlockingUi: Boolean
        get() = isSaving

    val canGenerate: Boolean
        get() = videoPath.isNotBlank() && selectedDurationMs in 1L..60_000L && !isProcessing

    val canSave: Boolean
        get() = selectedStickerCount > 0 && packName.isNotBlank() && publisher.isNotBlank() && !isProcessing
}

enum class VideoStickerPackProcessingStep {
    FindingFrames,
    BuildingGrids,
    AskingAi,
    PreparingPreview,
    Saving
}
