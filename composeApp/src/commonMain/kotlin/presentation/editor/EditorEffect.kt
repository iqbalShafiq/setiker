package presentation.editor

sealed interface EditorEffect {
    data object StickerSaved : EditorEffect
    data object NavigateBack : EditorEffect
    data class NavigateToCrop(val imagePath: String) : EditorEffect
    data class NavigateToBackgroundRemover(val imagePath: String) : EditorEffect
    data class ShowError(val message: String) : EditorEffect
}
