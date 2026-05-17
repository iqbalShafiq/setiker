package presentation.publicpack

import presentation.common.UiText

sealed interface PublicPackDetailEffect {
    data object NavigateBack : PublicPackDetailEffect
    data class NavigateToLocalPack(val localPackId: String) : PublicPackDetailEffect
    data object NavigateToLogin : PublicPackDetailEffect
    data class ShowMessage(val message: UiText) : PublicPackDetailEffect
}
