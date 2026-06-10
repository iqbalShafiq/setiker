package presentation.creator

import data.remote.ExploreSort
import data.remote.model.CloudStickerPack
import data.remote.model.PublicUserProfile

data class CreatorProfileState(
    val isLoading: Boolean = true,
    val profile: PublicUserProfile? = null,
    val packs: List<CloudStickerPack> = emptyList(),
    val sort: ExploreSort = ExploreSort.RECENT,
    val loadFailed: Boolean = false
)
