package presentation.createpack

import domain.model.StickerDecoration

/**
 * Sticker entry while editing a pack in Create Pack (before persist).
 *
 * For animated stickers ([isAnimated] = true), [imagePath] points to the final
 * encoded animated WebP file produced by the animated editor; the repository can
 * persist it directly without re-encoding.
 */
data class DraftSticker(
    val imagePath: String,
    val decorations: List<StickerDecoration> = emptyList(),
    val isAnimated: Boolean = false,
    val sourceVideoFile: String? = null,
    val frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap()
)
