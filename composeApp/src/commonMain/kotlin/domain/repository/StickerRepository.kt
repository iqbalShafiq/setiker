package domain.repository

import domain.model.StickerPack

interface StickerRepository {
    suspend fun getAllPacks(): List<StickerPack>
    suspend fun getPack(identifier: String): StickerPack
    suspend fun savePack(pack: StickerPack)
    suspend fun deletePack(identifier: String)
    suspend fun addStickerToPack(packId: String, sticker: domain.model.Sticker)
    suspend fun removeStickerFromPack(packId: String, index: Int)
}
