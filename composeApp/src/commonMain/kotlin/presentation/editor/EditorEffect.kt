package presentation.editor

import presentation.common.UiText

sealed interface EditorEffect {
    data object StickerSaved : EditorEffect
    data object NavigateBack : EditorEffect
    data class NavigateToCrop(val imagePath: String) : EditorEffect
    data class ShowError(val message: UiText) : EditorEffect
    data class ShowMessage(val message: UiText) : EditorEffect
}
