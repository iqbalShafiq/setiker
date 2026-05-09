package presentation.packdetail

import domain.model.StickerPack

data class PackDetailState(
    val isLoading: Boolean = false,
    val pack: StickerPack? = null,
    val error: String? = null,
    val isAddedToWhatsApp: Boolean = false,
    val stickerImportQueue: List<String> = emptyList(),
    /** Original multi-select count; used for success copy when the queue is finished. */
    val stickerImportBatchTotal: Int = 0
)
