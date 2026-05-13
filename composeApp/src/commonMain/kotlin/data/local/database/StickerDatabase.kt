package data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import data.local.entity.PendingSyncOperationEntity
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity

@Database(
    entities = [
        StickerPackEntity::class,
        StickerEntity::class,
        PendingSyncOperationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class StickerDatabase : RoomDatabase() {
    abstract fun stickerPackDao(): StickerPackDao
    abstract fun stickerDao(): StickerDao
    abstract fun syncOperationDao(): SyncOperationDao

    companion object {
        const val DATABASE_NAME = "sticker_database.db"
    }
}