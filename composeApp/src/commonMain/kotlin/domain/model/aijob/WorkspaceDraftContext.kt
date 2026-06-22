package domain.model.aijob

import domain.model.StickerDecoration
import domain.model.VideoStickerCandidateManifestItem
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceDraftContext(
    val prompt: String = "",
    val inputImagePath: String? = null,
    val videoPath: String? = null,
    val packName: String = "",
    val publisher: String = "",
    val visibility: String = "PRIVATE",
    val trayImagePath: String = "",
    val layout: String = "4x4",
    val gridLayout: String? = null,
    val gridSplitSourcePath: String = "",
    val selectedStartMs: Long = 0L,
    val selectedEndMs: Long = 0L,
    val sourceDurationMs: Long = 0L,
    val stickers: List<DraftStickerSnapshot> = emptyList(),
    val generatedPreview: List<DraftStickerSnapshot> = emptyList(),
    val selectedGeneratedPreview: Set<Int> = emptySet(),
    val generatedPreviewMode: String = "AddToPack",
    val splitPreview: List<DraftStickerSnapshot> = emptyList(),
    val selectedSplitPreview: Set<Int> = emptySet(),
    val backgroundPreviewPath: String? = null,
    val editorImagePath: String = "",
    val editorDecorations: List<StickerDecoration> = emptyList(),
    val candidatePaths: List<String> = emptyList(),
    val candidateGridPaths: List<String> = emptyList(),
    val candidateManifest: List<VideoStickerCandidateManifestItem> = emptyList(),
    val videoPlanResultJson: String? = null,
    val selectedStaticKeys: Set<String> = emptySet(),
    val selectedAnimatedKeys: Set<String> = emptySet(),
    val lastCompletedStep: String? = null,
    val tempFilePaths: List<String> = emptyList(),
    val homeAutoSaveOnComplete: Boolean = false,
    val resultPackId: String? = null,
    val animatedSpecJson: String? = null,
    val animatedFrameSnapshots: List<AnimatedFrameSnapshot> = emptyList(),
    val animatedBaseDecorations: List<StickerDecoration> = emptyList(),
    val animatedFrameDecorations: Map<Int, List<StickerDecoration>> = emptyMap(),
    val animatedEmojis: List<String> = emptyList(),
    val animatedAccessibilityText: String = "",
    val animatedApplyScope: String = "AllFrames",
    val animatedCurrentFrameIndex: Int = 0,
    val animatedOutputPath: String? = null
)

@Serializable
data class DraftStickerSnapshot(
    val imagePath: String,
    val decorations: List<StickerDecoration> = emptyList(),
    val isAnimated: Boolean = false,
    val sourceVideoFile: String? = null,
    val frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap()
)
