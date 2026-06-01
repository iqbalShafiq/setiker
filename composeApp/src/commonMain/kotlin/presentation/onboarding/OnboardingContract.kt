package presentation.onboarding

import presentation.common.UiText

data class OnboardingState(
    val pageIndex: Int = 0,
    val pageCount: Int = 3
)

sealed interface OnboardingIntent {
    data object Next : OnboardingIntent
    data class SelectPage(val pageIndex: Int) : OnboardingIntent
    data object Skip : OnboardingIntent
    data object Finish : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object NavigateToHome : OnboardingEffect
    data class ShowError(val message: UiText) : OnboardingEffect
}
