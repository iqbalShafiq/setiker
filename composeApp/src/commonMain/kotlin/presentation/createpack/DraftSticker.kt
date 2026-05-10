package presentation.createpack

import domain.model.StickerDecoration

/**
 * Sticker entry while editing a pack in Create Pack (before or after persist).
 */
data class DraftSticker(
    val imagePath: String,
    val decorations: List<StickerDecoration> = emptyList()
)
