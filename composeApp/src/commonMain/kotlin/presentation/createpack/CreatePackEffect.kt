package presentation.createpack

import presentation.common.UiText

sealed interface CreatePackEffect {
    data class PackSaved(val packId: String) : CreatePackEffect
    data class NavigateToPublicPack(val cloudPackId: String) : CreatePackEffect
    data class ShowSuccess(val message: String) : CreatePackEffect
    data object NavigateBack : CreatePackEffect
    data class ShowError(val message: UiText) : CreatePackEffect
}
