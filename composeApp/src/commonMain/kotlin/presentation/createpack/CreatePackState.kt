package presentation.createpack

import domain.model.AiUsage
import domain.model.StickerPack

enum class GridSplitSheetPhase {
    Hidden,
    ConfirmPick,
    Results
}

enum class GeneratedPreviewMode {
    AddToPack,
    ReplacePack
}

data class CreatePackState(
    val isLoading: Boolean = false,
    val name: String = "",
    val publisher: String = "",
    val visibility: String = "PRIVATE",
    val trayImagePath: String = "",
    val stickers: List<DraftSticker> = emptyList(),
    val generatePrompt: String = "",
    val gridLayout: String = "4x4",
    /**
     * Optional reference image for `/api/v1/generate`. In pack editor we default to null
     * (text-only generation); user can explicitly add a reference. Sticker editor defaults
     * to the related sticker's image.
     */
    val generateInputImage: String? = null,
    val generatedPreview: List<DraftSticker> = emptyList(),
    val selectedGeneratedPreview: Set<Int> = emptySet(),
    val generatedPreviewMode: GeneratedPreviewMode = GeneratedPreviewMode.AddToPack,
    /** When false, generated previews are kept but the results bottom sheet is hidden. */
    val generatedResultsSheetVisible: Boolean = false,
    val improveConfirmVisible: Boolean = false,
    val gridSplitSourcePath: String = "",
    val splitPreview: List<DraftSticker> = emptyList(),
    val selectedSplitPreview: Set<Int> = emptySet(),
    val isApiLoading: Boolean = false,
    val isSaving: Boolean = false,
    val aiGenerateSheetOpen: Boolean = false,
    val gridSplitSheetPhase: GridSplitSheetPhase = GridSplitSheetPhase.Hidden,
    val isEditing: Boolean = false,
    val packId: String = "",
    val error: String? = null,
    /** Raw gallery path; user must confirm crop before AddSticker / tray update. */
    val pendingStickerGalleryPath: String? = null,
    val pendingTrayGalleryPath: String? = null,
    val workspaceDraftId: String? = null,
    val backgroundJobMessage: String? = null,
    val backgroundJobProgress: Float = 0f,
    val aiUsage: AiUsage? = null,
    val isLoadingAiUsage: Boolean = false,
    val aiUsageLoadFailed: Boolean = false,
    val cloudId: String? = null,
    val isPublishing: Boolean = false,
    val presetPickerVisible: Boolean = false,
    val showContentPolicySheet: Boolean = false,
    val showAiOutputReportSheet: Boolean = false,
    val aiOutputReportReason: String? = null,
    val aiOutputReportDetails: String = "",
    val isSubmittingAiOutputReport: Boolean = false
) {
    val canPublishToExplore: Boolean
        get() = name.isNotBlank() &&
            publisher.isNotBlank() &&
            trayImagePath.isNotBlank() &&
            stickers.size >= StickerPack.MIN_STICKERS

    /**
     * Pack is treated as animated whenever at least one sticker is animated. This is decided
     * implicitly — there is no separate Static/Animated pack mode toggle anymore. Static
     * stickers in such a pack are re-encoded to 1-frame animated WebP at save time so the
     * WhatsApp `animated_sticker_pack` contract holds.
     */
    val containsAnimated: Boolean get() = stickers.any { it.isAnimated }
}
