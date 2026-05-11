package presentation.packdetail

import domain.model.StickerPack

data class PackDetailState(
    // Default to true so the first composition shows the loading indicator
    // instead of the "Pack not found" empty state. Initial render happens
    // before LoadPack reaches the ViewModel, and we never want to flash the
    // not-found copy on a pack we're literally about to load.
    val isLoading: Boolean = true,
    val pack: StickerPack? = null,
    val error: String? = null,
    val isAddedToWhatsApp: Boolean = false,
    val stickerImportQueue: List<String> = emptyList(),
    /** Original multi-select count; used for success copy when the queue is finished. */
    val stickerImportBatchTotal: Int = 0
)
