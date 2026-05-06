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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE packId = :packId AND sortOrder = :index")
    suspend fun deleteByPackAndIndex(packId: String, index: Int)

    @Query("DELETE FROM stickers WHERE packId = :packId")
    suspend fun deleteByPackId(packId: String)

    @Query("SELECT COUNT(*) FROM stickers WHERE packId = :packId")
    suspend fun getCountByPackId(packId: String): Int
}