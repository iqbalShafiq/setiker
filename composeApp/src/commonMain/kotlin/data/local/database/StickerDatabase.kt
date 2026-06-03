package data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import data.local.entity.AiJobEntity
import data.local.entity.PendingSyncOperationEntity
import data.local.entity.StickerEntity
import data.local.dao.ProcessingHistoryCacheDao
import data.local.entity.StickerPackEntity
import data.local.entity.ProcessingHistoryCacheEntity
import data.local.entity.WorkspaceDraftEntity

@Database(
    entities = [
        StickerPackEntity::class,
        StickerEntity::class,
        PendingSyncOperationEntity::class,
        AiJobEntity::class,
        WorkspaceDraftEntity::class,
        ProcessingHistoryCacheEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class StickerDatabase : RoomDatabase() {
    abstract fun stickerPackDao(): StickerPackDao
    abstract fun stickerDao(): StickerDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun aiJobDao(): AiJobDao
    abstract fun workspaceDraftDao(): WorkspaceDraftDao
    abstract fun processingHistoryCacheDao(): ProcessingHistoryCacheDao

    companion object {
        const val DATABASE_NAME = "sticker_database.db"
    }
}