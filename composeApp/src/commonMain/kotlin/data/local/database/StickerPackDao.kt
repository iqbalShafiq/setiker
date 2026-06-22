package data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import data.local.entity.StickerPackEntity

@Dao
interface StickerPackDao {
    @Query("SELECT * FROM sticker_packs ORDER BY updatedAt DESC")
    suspend fun getAll(): List<StickerPackEntity>

    @Query("SELECT * FROM sticker_packs WHERE identifier = :id")
    suspend fun getById(id: String): StickerPackEntity?

    /**
     * Inserts a brand-new pack row only. Do not use to update an existing pack:
     * [OnConflictStrategy.REPLACE] deletes the old row first, which cascades and
     * wipes child sticker rows (see [StickerEntity] FK).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: StickerPackEntity)

    @Update
    suspend fun update(pack: StickerPackEntity)

    /** Updates metadata when the pack already exists; inserts when it does not. */
    @Transaction
    suspend fun upsert(pack: StickerPackEntity) {
        if (getById(pack.identifier) != null) {
            update(pack)
        } else {
            insert(pack)
        }
    }

     @Delete
     suspend fun delete(pack: StickerPackEntity)
 
     // Sync queries
     @Query("SELECT * FROM sticker_packs WHERE syncState = :state")
     suspend fun getBySyncState(state: String): List<StickerPackEntity>
 
     @Query("SELECT * FROM sticker_packs WHERE syncState NOT IN ('SYNCED', 'SUCCESS')")
     suspend fun getUnsynced(): List<StickerPackEntity>
 
     @Query("SELECT * FROM sticker_packs WHERE cloudId = :cloudId")
     suspend fun getByCloudId(cloudId: String): StickerPackEntity?
 
     @Query("UPDATE sticker_packs SET syncState = :syncState, lastSyncAt = :lastSyncAt WHERE identifier = :identifier")
     suspend fun updateSyncStatus(identifier: String, syncState: String, lastSyncAt: Long?)
 
     @Query("UPDATE sticker_packs SET cloudId = :cloudId WHERE identifier = :identifier")
     suspend fun updateCloudId(identifier: String, cloudId: String)
 }
