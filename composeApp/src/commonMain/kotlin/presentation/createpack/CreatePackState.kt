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
    val stickers: List<String> = emptyList(),
    val generatePrompt: String = "",
    val generateAsGrid: Boolean = true,
    val gridLayout: String = "4x4",
    val normalizeOutput: Boolean = true,
    val generatedPreview: List<String> = emptyList(),
    val selectedGeneratedPreview: Set<Int> = emptySet(),
    val gridSplitSourcePath: String = "",
    val splitPreview: List<String> = emptyList(),
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
)
