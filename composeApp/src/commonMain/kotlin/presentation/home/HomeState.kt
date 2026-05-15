package presentation.home

import domain.model.SortOrder
import domain.model.StickerPack
import domain.model.User

data class HomeState(
    val isLoading: Boolean = false,
    val packs: List<StickerPack> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val error: String? = null,
    val currentUser: User? = null,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0
) {
    val filteredPacks: List<StickerPack>
        get() = packs
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(sortOrder.comparator)
}
