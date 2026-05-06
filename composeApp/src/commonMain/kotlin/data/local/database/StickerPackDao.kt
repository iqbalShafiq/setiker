package data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.local.entity.StickerPackEntity

@Dao
interface StickerPackDao {
    @Query("SELECT * FROM sticker_packs ORDER BY updatedAt DESC")
    suspend fun getAll(): List<StickerPackEntity>

    @Query("SELECT * FROM sticker_packs WHERE identifier = :id")
    suspend fun getById(id: String): StickerPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: StickerPackEntity)

    @Delete
    suspend fun delete(pack: StickerPackEntity)
}