package presentation.packdetail

import presentation.common.UiText

sealed interface PackDetailEffect {
    data object NavigateBack : PackDetailEffect
    data object NavigateToEditPack : PackDetailEffect
    data object NavigateToAddSticker : PackDetailEffect
    data class NavigateToEditSticker(val index: Int) : PackDetailEffect
    data class ShowError(val message: UiText) : PackDetailEffect
    data class ShowSuccess(val message: UiText) : PackDetailEffect
    data class LaunchAddToWhatsApp(val packId: String, val packName: String) : PackDetailEffect
    data class ShareText(val text: String) : PackDetailEffect
    data class NavigateToDuplicatedPack(val packId: String) : PackDetailEffect
    data class NavigateToPublicPack(val cloudPackId: String) : PackDetailEffect
}
