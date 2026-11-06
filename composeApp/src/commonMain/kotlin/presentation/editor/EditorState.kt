package presentation.editor

import domain.model.AiUsage
import domain.model.StickerDecoration
import presentation.createpack.DraftSticker

data class EditorState(
    val imagePath: String = "",
    val emojis: List<String> = emptyList(),
    val accessibilityText: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /**
     * The last save succeeded and its confirmation is still on screen.
     *
     * [isSaving] is only true *while* saving, so it cannot drive a "finished" state: by the time
     * the write returns, the screen is already on its way out. This flag is what lets the save
     * button show a tick instead of silently swapping back to its idle glyph.
     */
    val isSaveConfirmed: Boolean = false,
    val error: String? = null,
    val showEmojiPicker: Boolean = false,
    val recentEmojis: List<String> = emptyList(),
    val packId: String = "",
    val stickerIndex: Int? = null,
    val isBackgroundRemoverSheetOpen: Boolean = false,
    val isBackgroundRemoving: Boolean = false,
    val backgroundRemoverPreviewPath: String? = null,
    val decorations: List<StickerDecoration> = emptyList(),
    val selectedDecorationId: String? = null,
    val showDecorationEmojiPicker: Boolean = false,
    val decorationEmojiPickerTargetId: String? = null,
    val isTextDecorationSheetOpen: Boolean = false,
    // AI generate mirrors the contract used in `CreatePackState`.
    val aiGenerateSheetOpen: Boolean = false,
    val generatePrompt: String = "",
    /**
     * Optional reference image for `/api/v1/generate`. The sticker editor defaults this to the
     * current sticker image when the sheet opens; the user can override from gallery or clear.
     */
    val generateInputImage: String? = null,
    val isApiLoading: Boolean = false,
    /** Generated images returned by the API, shown in the replacement picker sheet. */
    val generatedPreview: List<DraftSticker> = emptyList(),
    val generatedResultsSheetVisible: Boolean = false,
    val improveConfirmVisible: Boolean = false,
    val removeBackgroundConfirmVisible: Boolean = false,
    val workspaceDraftId: String? = null,
    val backgroundJobMessage: String? = null,
    val aiUsage: AiUsage? = null,
    val isLoadingAiUsage: Boolean = false,
    val aiUsageLoadFailed: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showAiOutputReportSheet: Boolean = false,
    val aiOutputReportReason: String? = null,
    val aiOutputReportDetails: String = "",
    val isSubmittingAiOutputReport: Boolean = false
)
