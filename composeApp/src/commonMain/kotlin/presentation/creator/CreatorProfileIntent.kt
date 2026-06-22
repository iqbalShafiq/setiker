package presentation.creator

import data.remote.ExploreSort

sealed interface CreatorProfileIntent {
    data class Load(val userId: String) : CreatorProfileIntent
    data class ChangeSort(val sort: ExploreSort) : CreatorProfileIntent
    data object ToggleFollow : CreatorProfileIntent
    data class OpenPack(val packId: String) : CreatorProfileIntent
    data object NavigateBack : CreatorProfileIntent
}
