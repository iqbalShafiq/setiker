package presentation.packdetail

import domain.model.StickerPack

data class PackDetailState(
    val isLoading: Boolean = false,
    val pack: StickerPack? = null,
    val error: String? = null,
    val isAddedToWhatsApp: Boolean = false
)
