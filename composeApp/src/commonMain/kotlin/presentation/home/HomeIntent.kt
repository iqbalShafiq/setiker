package presentation.home

import domain.model.SortOrder

sealed interface HomeIntent {
    data object LoadPacks : HomeIntent
    data class DeletePack(val packId: String) : HomeIntent
    data class AddToWhatsApp(val packId: String) : HomeIntent
    data object CreateNewPack : HomeIntent
    data object NavigateToProfile : HomeIntent
    data object NavigateToSync : HomeIntent
    data object NavigateToExplore : HomeIntent
    data object NavigateToLogin : HomeIntent
    data object RefreshSync : HomeIntent
    data class SearchQueryChanged(val query: String) : HomeIntent
    data class SortOrderChanged(val order: SortOrder) : HomeIntent
}
