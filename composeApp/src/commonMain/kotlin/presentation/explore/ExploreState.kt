package presentation.explore

import data.remote.ExploreFeed
import data.remote.ExploreSort
import data.remote.model.CloudStickerPack

data class ExploreState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val packs: List<CloudStickerPack> = emptyList(),
    val searchQuery: String = "",
    val page: Int = 1,
    val limit: Int = 20,
    val totalPages: Int = 1,
    val sort: ExploreSort = ExploreSort.RECENT,
    val feed: ExploreFeed = ExploreFeed.DISCOVER,
    val featuredPack: CloudStickerPack? = null,
    val requiresLogin: Boolean = false
) {
    val canLoadMore: Boolean
        get() = page < totalPages && !isLoadingMore && !requiresLogin
}
