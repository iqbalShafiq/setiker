package presentation.explore

import presentation.common.UiText

sealed interface ExploreEffect {
    data class NavigateToPublicPack(val packId: String) : ExploreEffect
    data object NavigateBack : ExploreEffect
    data object NavigateHistory : ExploreEffect
    data class ShowError(val message: UiText) : ExploreEffect
}
