package presentation.home

import domain.model.StickerPack

data class HomeState(
    val isLoading: Boolean = false,
    val packs: List<StickerPack> = emptyList(),
    val error: String? = null
)
