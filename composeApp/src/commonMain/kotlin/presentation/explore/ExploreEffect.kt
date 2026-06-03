package presentation.explore

import presentation.common.UiText

sealed interface ExploreEffect {
    data class ShowError(val message: UiText) : ExploreEffect
    data class NavigateToPublicPack(val packId: String) : ExploreEffect
    data class NavigateToCreator(val userId: String) : ExploreEffect
    data object NavigateBack : ExploreEffect
    data object NavigateHistory : ExploreEffect
    data object NavigateLogin : ExploreEffect
}
