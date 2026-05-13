package presentation.home

sealed interface HomeIntent {
    data object LoadPacks : HomeIntent
    data class DeletePack(val packId: String) : HomeIntent
    data class AddToWhatsApp(val packId: String) : HomeIntent
    data object CreateNewPack : HomeIntent
    data object NavigateToProfile : HomeIntent
    data object NavigateToSync : HomeIntent
    data object NavigateToLogin : HomeIntent
    data object RefreshSync : HomeIntent
}
