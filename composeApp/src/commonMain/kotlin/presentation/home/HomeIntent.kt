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
    data object OpenGeneratePackSheet : HomeIntent
    data object CloseGeneratePackSheet : HomeIntent
    data class UpdateGeneratePackPrompt(val prompt: String) : HomeIntent
    data class UpdateGeneratePackName(val name: String) : HomeIntent
    data class UpdateGeneratePackPublisher(val publisher: String) : HomeIntent
    data class UpdateGeneratePackLayout(val layout: String) : HomeIntent
    data class UpdateGeneratePackInputImage(val path: String?) : HomeIntent
    data object GenerateStickerPack : HomeIntent
}
