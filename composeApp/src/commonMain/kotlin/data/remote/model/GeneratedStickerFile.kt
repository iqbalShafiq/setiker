package data.remote.model

import domain.model.StickerDecoration

data class GeneratedStickerFile(
    val localPath: String,
    val decorations: List<StickerDecoration> = emptyList()
)
