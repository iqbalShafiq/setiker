package presentation.home

import domain.model.AiUsage
import domain.model.SortOrder
import domain.model.StickerPack
import domain.model.User

data class HomeState(
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val packs: List<StickerPack> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val error: String? = null,
    val currentUser: User? = null,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0,
    val isGeneratePackSheetOpen: Boolean = false,
    val generatePackPrompt: String = "",
    val generatePackName: String = "",
    val generatePackPublisher: String = "",
    val generatePackLayout: String = "4x4",
    val generatePackInputImagePath: String? = null,
    val isGeneratePackLoading: Boolean = false,
    val aiUsage: AiUsage? = null,
    val isLoadingAiUsage: Boolean = false,
    val aiUsageLoadFailed: Boolean = false,
    val activeAiJobCount: Int = 0,
    val aiJobsBadgeCount: Int = 0,
    val backgroundJobMessage: String? = null,
    val homeWorkspaceDraftId: String? = null,
    val processingPacks: List<HomeProcessingPack> = emptyList()
) {
    val filteredPacks: List<StickerPack>
        get() = packs
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(sortOrder.comparator)

    val filteredProcessingPacks: List<HomeProcessingPack>
        get() = processingPacks
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }

    val hasListContent: Boolean
        get() = filteredPacks.isNotEmpty() || filteredProcessingPacks.isNotEmpty()
}
