package presentation.editor

import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import presentation.createpack.DraftSticker

sealed interface EditorIntent {
    data class UpdateImagePath(val path: String) : EditorIntent
    data class AddEmoji(val emoji: String) : EditorIntent
    data class RemoveEmoji(val index: Int) : EditorIntent
    data class UpdateAccessibilityText(val text: String) : EditorIntent
    data object SaveSticker : EditorIntent
    data object NavigateToCrop : EditorIntent
    data object RequestRemoveBackground : EditorIntent
    data object ConfirmRemoveBackground : EditorIntent
    data object DismissRemoveBackgroundConfirm : EditorIntent
    data object DismissBackgroundRemoverSheet : EditorIntent
    data object ConfirmBackgroundRemoval : EditorIntent
    data class SetPackId(val packId: String, val stickerIndex: Int? = null) : EditorIntent
    data class LoadSticker(val stickerIndex: Int, val packId: String) : EditorIntent
    data object ShowEmojiPicker : EditorIntent
    data object HideEmojiPicker : EditorIntent
    data class LoadRecentEmojis(val emojis: List<String>) : EditorIntent
    data class AddImageDecorationFromGallery(val path: String) : EditorIntent
    data class AddTextDecoration(val text: String, val font: DecorationFont) : EditorIntent
    data class AddEmojiDecoration(val emoji: String) : EditorIntent
    data class UpdateDecorationTransform(
        val id: String,
        val centerX: Float,
        val centerY: Float,
        val scale: Float
    ) : EditorIntent
    data class SelectDecoration(val id: String?) : EditorIntent
    data class RemoveDecoration(val id: String) : EditorIntent
    data class ShowDecorationEmojiPicker(val targetDecorationId: String? = null) : EditorIntent
    data object HideDecorationEmojiPicker : EditorIntent
    data object ShowTextDecorationSheet : EditorIntent
    data object HideTextDecorationSheet : EditorIntent
    data class UpdateTextDecorationText(val id: String, val text: String) : EditorIntent
    data class UpdateTextDecorationFont(val id: String, val font: DecorationFont) : EditorIntent
    data class UpdateTextDecorationFontWeight(val id: String, val fontWeight: DecorationFontWeight) : EditorIntent
    data class UpdateTextDecorationColor(val id: String, val colorArgb: Long) : EditorIntent
    data class UpdateTextDecorationBorderColor(val id: String, val colorArgb: Long) : EditorIntent
    data class UpdateTextDecorationBorderWidth(val id: String, val widthRatio: Float) : EditorIntent
    data class UpdateEmojiDecorationValue(val id: String, val emoji: String) : EditorIntent
    data class UpdateEmojiDecorationBorderColor(val id: String, val colorArgb: Long) : EditorIntent
    data class UpdateEmojiDecorationBorderWidth(val id: String, val widthRatio: Float) : EditorIntent
    data class UpdateImageDecorationPath(val id: String, val imagePath: String) : EditorIntent
    // AI generate flow — mirrors `CreatePackIntent` for shared bottom sheet wiring.
    data object OpenAiGenerateSheet : EditorIntent
    data object CloseAiGenerateSheet : EditorIntent
    data class UpdateGeneratePrompt(val prompt: String) : EditorIntent
    data class UpdateGenerateInputImage(val path: String?) : EditorIntent
    data object GenerateSticker : EditorIntent
    data object RequestImproveSticker : EditorIntent
    data object ConfirmImproveSticker : EditorIntent
    data object DismissImproveConfirm : EditorIntent
    data class ApplyGeneratedSticker(val draft: DraftSticker) : EditorIntent
    data class RestoreWorkspaceDraft(val draftId: String) : EditorIntent
    data object DismissGeneratedResultsSheet : EditorIntent
    data object CancelGeneratedResults : EditorIntent
    data object ShowGeneratedResultsSheet : EditorIntent
    data object Undo : EditorIntent
    data object Redo : EditorIntent
}
