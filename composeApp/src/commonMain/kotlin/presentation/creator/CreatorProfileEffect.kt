package presentation.creator

import presentation.common.UiText

sealed interface CreatorProfileEffect {
    data object NavigateBack : CreatorProfileEffect
    data class NavigateToPack(val packId: String) : CreatorProfileEffect
    data class ShowMessage(val message: UiText) : CreatorProfileEffect
}
