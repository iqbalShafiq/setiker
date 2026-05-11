package presentation.createpack

enum class GridSplitSheetPhase {
    Hidden,
    ConfirmPick,
    Results
}

data class CreatePackState(
    val isLoading: Boolean = false,
    val name: String = "",
    val publisher: String = "",
    val trayImagePath: String = "",
    val stickers: List<DraftSticker> = emptyList(),
    val generatePrompt: String = "",
    val generateAsGrid: Boolean = true,
    val gridLayout: String = "4x4",
    val normalizeOutput: Boolean = true,
    /**
     * Optional reference image for `/api/v1/generate`. In pack editor we default to null
     * (text-only generation); user can explicitly add a reference. Sticker editor defaults
     * to the related sticker's image.
     */
    val generateInputImage: String? = null,
    val generatedPreview: List<String> = emptyList(),
    val selectedGeneratedPreview: Set<Int> = emptySet(),
    val gridSplitSourcePath: String = "",
    val splitPreview: List<DraftSticker> = emptyList(),
    val selectedSplitPreview: Set<Int> = emptySet(),
    val isApiLoading: Boolean = false,
    val aiGenerateSheetOpen: Boolean = false,
    val gridSplitSheetPhase: GridSplitSheetPhase = GridSplitSheetPhase.Hidden,
    val isEditing: Boolean = false,
    val packId: String = "",
    val error: String? = null,
    /** Raw gallery path; user must confirm crop before AddSticker / tray update. */
    val pendingStickerGalleryPath: String? = null,
    val pendingTrayGalleryPath: String? = null
) {
    /**
     * Pack is treated as animated whenever at least one sticker is animated. This is decided
     * implicitly — there is no separate Static/Animated pack mode toggle anymore. Static
     * stickers in such a pack are re-encoded to 1-frame animated WebP at save time so the
     * WhatsApp `animated_sticker_pack` contract holds.
     */
    val containsAnimated: Boolean get() = stickers.any { it.isAnimated }
}
