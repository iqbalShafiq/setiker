package presentation.explore

import data.remote.ExploreSort
import data.remote.model.CloudStickerPack

data class ExploreState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isImporting: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val packs: List<CloudStickerPack> = emptyList(),
    val searchQuery: String = "",
    val page: Int = 1,
    val limit: Int = 20,
    val totalPages: Int = 1,
    val sort: ExploreSort = ExploreSort.RECENT
) {
    val filteredPacks: List<CloudStickerPack>
        get() {
            if (searchQuery.isBlank()) return packs
            val needle = searchQuery.trim().lowercase()
            return packs.filter { pack ->
                pack.name.lowercase().contains(needle) ||
                    (pack.owner?.displayName?.lowercase()?.contains(needle) == true) ||
                    (pack.owner?.username?.lowercase()?.contains(needle) == true)
            }
        }

    val canLoadMore: Boolean
        get() = page < totalPages && !isLoadingMore
}
