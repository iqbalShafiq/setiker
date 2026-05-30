package presentation.videostickerpack

import domain.model.CandidateGridImage
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.VideoFrameCandidate
import domain.model.VideoStickerCandidateManifestItem

data class VideoStickerPackPreviewFrame(
    val timestampMs: Long,
    val filePath: String
)

data class VideoStickerPackState(
    val videoPath: String = "",
    val previewFramePath: String? = null,
    val previewFrames: List<VideoStickerPackPreviewFrame> = emptyList(),
    val currentPreviewIndex: Int = 0,
    val isPreviewPlaying: Boolean = false,
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
    val errorMessage: String? = null,
    val workspaceDraftId: String? = null,
    val backgroundJobMessage: String? = null
) {
    val selectedDurationMs: Long
        get() = selectedEndMs - selectedStartMs

    val previewFramesInRange: List<VideoStickerPackPreviewFrame>
        get() = previewFrames.filter { it.timestampMs in selectedStartMs..selectedEndMs }

    val activePreviewFrames: List<VideoStickerPackPreviewFrame>
        get() = previewFramesInRange.ifEmpty { previewFrames }

    val currentPreviewFrame: VideoStickerPackPreviewFrame?
        get() = activePreviewFrames.getOrNull(
            currentPreviewIndex.coerceIn(0, (activePreviewFrames.size - 1).coerceAtLeast(0))
        )

    val currentPreviewTimestampMs: Long
        get() = currentPreviewFrame?.timestampMs ?: selectedStartMs

    val currentPreviewPathResolved: String?
        get() = currentPreviewFrame?.filePath ?: previewFramePath

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

    val canPlayPreview: Boolean
        get() = activePreviewFrames.size > 1 && !isLoadingVideo && !isProcessing

    companion object {
        const val PREVIEW_FRAME_COUNT = 24
    }
}

enum class VideoStickerPackProcessingStep {
    FindingFrames,
    BuildingGrids,
    AskingAi,
    PreparingPreview,
    Saving
}
