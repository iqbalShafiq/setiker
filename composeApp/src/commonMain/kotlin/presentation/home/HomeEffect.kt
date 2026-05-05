package presentation.home

sealed interface HomeEffect {
    data class NavigateToPackDetail(val packId: String) : HomeEffect
    data object NavigateToCreatePack : HomeEffect
    data class ShowError(val message: String) : HomeEffect
    data class ShowSuccess(val message: String) : HomeEffect
}
