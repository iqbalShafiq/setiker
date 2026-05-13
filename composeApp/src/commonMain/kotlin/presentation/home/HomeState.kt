package presentation.home

import domain.model.StickerPack
import domain.model.User

data class HomeState(
    val isLoading: Boolean = false,
    val packs: List<StickerPack> = emptyList(),
    val error: String? = null,
    val currentUser: User? = null,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0
)
