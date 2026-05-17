package presentation.sharepreview

import presentation.common.UiText

sealed interface SharePreviewEffect {
    data object NavigateBack : SharePreviewEffect
    data object NavigateToLogin : SharePreviewEffect
    data class NavigateToLocalPack(val packId: String) : SharePreviewEffect
    data class ShowMessage(val message: UiText) : SharePreviewEffect
}
