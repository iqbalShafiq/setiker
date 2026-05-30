package domain.model.aijob

import kotlinx.serialization.Serializable

@Serializable
data class GenerateStickersPayload(
    val prompt: String,
    val inputImagePath: String? = null
)

@Serializable
data class GeneratePackPayload(
    val prompt: String,
    val layout: String,
    val packName: String,
    val publisher: String,
    val inputImagePath: String? = null
)

@Serializable
data class ImproveStickersPayload(
    val imagePaths: List<String>
)

@Serializable
data class GridSplitPayload(
    val imagePath: String,
    val layout: String?
)

@Serializable
data class RemoveBackgroundPayload(
    val imagePath: String
)

@Serializable
data class VideoPackPayload(
    val videoPath: String,
    val selectedStartMs: Long,
    val selectedEndMs: Long,
    val sourceDurationMs: Long,
    val prompt: String? = null,
    val extractFreshCandidates: Boolean = true
)

@Serializable
data class GenerateStickersResult(
    val previews: List<DraftStickerSnapshot>
)

@Serializable
data class GeneratePackResult(
    val packId: String
)

@Serializable
data class ImproveStickersResult(
    val previews: List<DraftStickerSnapshot>,
    val replaceMode: Boolean = false
)

@Serializable
data class GridSplitResult(
    val previews: List<DraftStickerSnapshot>
)

@Serializable
data class RemoveBackgroundResult(
    val outputPath: String
)

@Serializable
data class VideoPackCheckpoint(
    val candidatePaths: List<String> = emptyList(),
    val candidateGridPaths: List<String> = emptyList(),
    val candidateManifest: List<domain.model.VideoStickerCandidateManifestItem> = emptyList(),
    val videoPlanResultJson: String? = null
)

@Serializable
data class VideoPackResult(
    val videoPlanResultJson: String
)

@Serializable
data class AnimatedEncodePayload(
    val outputFileName: String
)

@Serializable
data class AnimatedEncodeResult(
    val outputPath: String
)
