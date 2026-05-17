package presentation.animatededitor

import domain.model.DecorationFont
import domain.model.DecorationFontWeight

sealed interface AnimatedEditorIntent {
    data class LoadDraft(val draftId: String) : AnimatedEditorIntent
    data class ScrubToFrame(val index: Int) : AnimatedEditorIntent
    data object PlayPreview : AnimatedEditorIntent
    data object PausePreview : AnimatedEditorIntent
    data object AdvanceFrame : AnimatedEditorIntent
    data class SetApplyScope(val scope: DecorationApplyScope) : AnimatedEditorIntent
    data class AddTextDecoration(val text: String, val font: DecorationFont) : AnimatedEditorIntent
    data class AddEmojiDecoration(val emoji: String) : AnimatedEditorIntent
    data class AddImageDecoration(val imagePath: String) : AnimatedEditorIntent
    data class SelectDecoration(val id: String?) : AnimatedEditorIntent
    data class RemoveDecoration(val id: String) : AnimatedEditorIntent
    data class UpdateDecorationTransform(
        val id: String,
        val centerX: Float,
        val centerY: Float,
        val scale: Float
    ) : AnimatedEditorIntent
    data class UpdateTextDecorationText(val id: String, val text: String) : AnimatedEditorIntent
    data class UpdateTextDecorationFont(val id: String, val font: DecorationFont) : AnimatedEditorIntent
    data class UpdateTextDecorationFontWeight(val id: String, val weight: DecorationFontWeight) : AnimatedEditorIntent
    data class UpdateTextDecorationColor(val id: String, val colorArgb: Long) : AnimatedEditorIntent
    data class UpdateTextDecorationBorderColor(val id: String, val colorArgb: Long) : AnimatedEditorIntent
    data class UpdateTextDecorationBorderWidth(val id: String, val widthRatio: Float) : AnimatedEditorIntent
    data class UpdateEmojiDecoration(val id: String, val emoji: String) : AnimatedEditorIntent
    data class UpdateEmojiDecorationBorderColor(val id: String, val colorArgb: Long) : AnimatedEditorIntent
    data class UpdateEmojiDecorationBorderWidth(val id: String, val widthRatio: Float) : AnimatedEditorIntent
    data class UpdateImageDecorationPath(val id: String, val imagePath: String) : AnimatedEditorIntent
    data class AddEmojiTag(val emoji: String) : AnimatedEditorIntent
    data class RemoveEmojiTag(val index: Int) : AnimatedEditorIntent
    data class UpdateAccessibilityText(val text: String) : AnimatedEditorIntent
    data object ShowEmojiPicker : AnimatedEditorIntent
    data object HideEmojiPicker : AnimatedEditorIntent
    data class ShowDecorationEmojiPicker(val targetDecorationId: String? = null) : AnimatedEditorIntent
    data object HideDecorationEmojiPicker : AnimatedEditorIntent
    data object ShowTextDecorationSheet : AnimatedEditorIntent
    data object HideTextDecorationSheet : AnimatedEditorIntent
    data class SetPackId(val packId: String) : AnimatedEditorIntent
    data object Save : AnimatedEditorIntent
}
