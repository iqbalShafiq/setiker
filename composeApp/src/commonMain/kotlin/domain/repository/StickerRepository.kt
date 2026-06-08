package domain.repository

import domain.model.Sticker
import domain.model.StickerPack
import domain.model.SyncReport
import kotlinx.coroutines.flow.Flow

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data object Pending : SyncStatus()
    data object Failed : SyncStatus()
}

interface StickerRepository {
    suspend fun getAllPacks(): List<StickerPack>
    suspend fun getPack(identifier: String): StickerPack
    suspend fun savePack(pack: StickerPack)
    suspend fun deletePack(identifier: String)
    suspend fun duplicatePack(identifier: String): String
    suspend fun addStickerToPack(packId: String, sticker: Sticker)
    suspend fun updateStickerInPack(packId: String, index: Int, sticker: Sticker)
    suspend fun removeStickerFromPack(packId: String, index: Int)
    suspend fun updatePackVisibility(packId: String, visibility: String)
    suspend fun syncAll(): SyncReport
    suspend fun syncPack(packId: String): SyncReport
    suspend fun clearCloudSyncedDataOnLogout()
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun getPendingSyncCount(): Int
}
