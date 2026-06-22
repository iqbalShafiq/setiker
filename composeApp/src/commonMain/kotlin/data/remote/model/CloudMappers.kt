package data.remote.model

import domain.model.Sticker
import domain.model.StickerPack
import kotlinx.datetime.Instant

/**
 * Mapper functions for converting cloud API models to domain models.
 */

fun CloudStickerPack.toDomainModel(stickers: List<Sticker> = emptyList()): StickerPack {
    return StickerPack(
        identifier = id,
        name = name,
        publisher = "", // Cloud packs might not have publisher info
        trayImageFile = "", // Will be populated from stickers or downloaded separately
        stickers = stickers,
        isAnimated = stickers.any { it.isAnimated }
    )
}
