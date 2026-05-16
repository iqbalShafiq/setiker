package data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.local.entity.StickerEntity

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers WHERE packId = :packId ORDER BY sortOrder")
    suspend fun getByPackId(packId: String): List<StickerEntity>

    @Query("SELECT * FROM stickers WHERE cloudId = :cloudId")
    suspend fun getByCloudId(cloudId: String): StickerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE packId = :packId AND sortOrder = :index")
    suspend fun deleteByPackAndIndex(packId: String, index: Int)

    @Query("DELETE FROM stickers WHERE packId = :packId")
    suspend fun deleteByPackId(packId: String)

    @Query("DELETE FROM stickers WHERE cloudId = :cloudId")
    suspend fun deleteByCloudId(cloudId: String)

    @Query("SELECT COUNT(*) FROM stickers WHERE packId = :packId")
    suspend fun getCountByPackId(packId: String): Int

    // Sync queries
    @Query("SELECT * FROM stickers WHERE syncState = :state")
    suspend fun getBySyncState(state: String): List<StickerEntity>

    @Query("SELECT * FROM stickers WHERE syncState != 'SYNCED'")
    suspend fun getUnsynced(): List<StickerEntity>

    @Query("UPDATE stickers SET syncState = :syncState, lastSyncAt = :lastSyncAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncState: String, lastSyncAt: Long?)

    @Query("UPDATE stickers SET cloudId = :cloudId, cloudUrl = :cloudUrl WHERE id = :id")
    suspend fun updateCloudInfo(id: String, cloudId: String, cloudUrl: String?)
}
