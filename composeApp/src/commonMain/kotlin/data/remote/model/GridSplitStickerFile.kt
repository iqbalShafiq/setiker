package data.remote.model

import domain.model.StickerDecoration

/**
 * Local file path after downloading a grid-split cell, plus optional overlay decorations.
 */
data class GridSplitStickerFile(
    val localPath: String,
    val rawCellPath: String? = null,
    val decorations: List<StickerDecoration> = emptyList()
)
