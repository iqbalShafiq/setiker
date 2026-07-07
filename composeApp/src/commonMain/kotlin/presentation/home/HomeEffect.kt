package presentation.home

import presentation.common.UiText

sealed interface HomeEffect {
    data class NavigateToPackDetail(val packId: String) : HomeEffect
    data object NavigateToCreatePack : HomeEffect
    data class ShowError(val message: UiText) : HomeEffect
    data class ShowSuccess(val message: UiText) : HomeEffect
    data object NavigateToProfile : HomeEffect
    data object NavigateToSync : HomeEffect
    data object NavigateToExplore : HomeEffect
    data object NavigateToLogin : HomeEffect
    data class NavigateToVideoStickerPack(val videoPath: String) : HomeEffect
    data object NavigateToAiJobs : HomeEffect
    data object NavigateToAiJobsFromTopBar : HomeEffect
    data object ShowQuotaExceeded : HomeEffect
    data object NavigateToPaywall : HomeEffect
}
