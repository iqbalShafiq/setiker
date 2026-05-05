package presentation.editor

sealed interface EditorIntent {
    data class UpdateImagePath(val path: String) : EditorIntent
    data class AddEmoji(val emoji: String) : EditorIntent
    data class RemoveEmoji(val index: Int) : EditorIntent
    data class UpdateAccessibilityText(val text: String) : EditorIntent
    data object SaveSticker : EditorIntent
    data object NavigateToCrop : EditorIntent
    data object NavigateToBackgroundRemover : EditorIntent
    data class LoadSticker(val stickerIndex: Int, val packId: String) : EditorIntent
}
