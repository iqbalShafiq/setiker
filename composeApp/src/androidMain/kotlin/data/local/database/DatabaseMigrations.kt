package data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE stickers ADD COLUMN decorationsJson TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE stickers ADD COLUMN sourceImageFile TEXT")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE sticker_packs ADD COLUMN isAnimated INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE stickers ADD COLUMN isAnimated INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE stickers ADD COLUMN sourceVideoFile TEXT")
            database.execSQL("ALTER TABLE stickers ADD COLUMN frameDecorationsJson TEXT")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add sync columns to sticker_packs
            database.execSQL("ALTER TABLE sticker_packs ADD COLUMN cloudId TEXT")
            database.execSQL("ALTER TABLE sticker_packs ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            database.execSQL("ALTER TABLE sticker_packs ADD COLUMN lastSyncAt INTEGER")
            database.execSQL("ALTER TABLE sticker_packs ADD COLUMN visibility TEXT NOT NULL DEFAULT 'PRIVATE'")
            database.execSQL("ALTER TABLE sticker_packs ADD COLUMN cloudOwnerId TEXT")

            // Add sync columns to stickers
            database.execSQL("ALTER TABLE stickers ADD COLUMN cloudId TEXT")
            database.execSQL("ALTER TABLE stickers ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            database.execSQL("ALTER TABLE stickers ADD COLUMN lastSyncAt INTEGER")
            database.execSQL("ALTER TABLE stickers ADD COLUMN cloudUrl TEXT")

            // Create sync_queue table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_queue (
                    id TEXT NOT NULL PRIMARY KEY,
                    type TEXT NOT NULL,
                    targetId TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    errorMessage TEXT,
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    completedAt INTEGER,
                    priority INTEGER NOT NULL DEFAULT 0
                )
            """)
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue(status)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_targetId ON sync_queue(targetId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue(createdAt)")
        }
    }
}
