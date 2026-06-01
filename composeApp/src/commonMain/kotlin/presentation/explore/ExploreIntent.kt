package presentation.explore

import data.remote.ExploreSort

sealed interface ExploreIntent {
    data object LoadInitial : ExploreIntent
    data object Refresh : ExploreIntent
    data object LoadMore : ExploreIntent
    data class ChangeSort(val sort: ExploreSort) : ExploreIntent
    data class SearchChanged(val query: String) : ExploreIntent
    data class OpenPack(val packId: String) : ExploreIntent
    data object NavigateBack : ExploreIntent
    data object NavigateHistory : ExploreIntent
}
