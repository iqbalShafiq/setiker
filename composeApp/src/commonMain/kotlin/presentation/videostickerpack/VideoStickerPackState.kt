package presentation.videostickerpack

import data.remote.model.GridSplitStickerFile
import domain.model.CandidateGridImage
import domain.model.VideoFrameCandidate

data class VideoStickerPackState(
    val videoPath: String = "",
    val sourceDurationMs: Long = 0L,
    val selectedStartMs: Long = 0L,
    val selectedEndMs: Long = 0L,
    val packName: String = "",
    val publisher: String = "",
    val isLoadingVideo: Boolean = false,
    val isProcessing: Boolean = false,
    val processingStep: VideoStickerPackProcessingStep? = null,
    val processingProgress: Float = 0f,
    val candidates: List<VideoFrameCandidate> = emptyList(),
    val candidateGrids: List<CandidateGridImage> = emptyList(),
    val generatedStickers: List<GridSplitStickerFile> = emptyList(),
    val errorMessage: String? = null
) {
    val selectedDurationMs: Long get() = selectedEndMs - selectedStartMs

    val canGenerate: Boolean
        get() = videoPath.isNotBlank() && selectedDurationMs in 1L..60_000L && !isProcessing

    val canSave: Boolean
        get() = generatedStickers.isNotEmpty() && packName.isNotBlank() && publisher.isNotBlank() && !isProcessing
}

enum class VideoStickerPackProcessingStep {
    FindingFrames,
    BuildingGrids,
    AskingAi
}
