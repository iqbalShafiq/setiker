package presentation.backgroundremover

import presentation.common.UiText

sealed interface BackgroundRemoverEffect {
    data class BackgroundRemoved(val path: String) : BackgroundRemoverEffect
    data object NavigateBack : BackgroundRemoverEffect
    data class ShowError(val message: UiText) : BackgroundRemoverEffect
}
