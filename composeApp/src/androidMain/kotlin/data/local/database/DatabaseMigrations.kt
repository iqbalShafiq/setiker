package data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE stickers ADD COLUMN decorationsJson TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE stickers ADD COLUMN sourceImageFile TEXT")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sticker_packs ADD COLUMN isAnimated INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE stickers ADD COLUMN isAnimated INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE stickers ADD COLUMN sourceVideoFile TEXT")
            db.execSQL("ALTER TABLE stickers ADD COLUMN frameDecorationsJson TEXT")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add sync columns to sticker_packs
            db.execSQL("ALTER TABLE sticker_packs ADD COLUMN cloudId TEXT")
            db.execSQL("ALTER TABLE sticker_packs ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            db.execSQL("ALTER TABLE sticker_packs ADD COLUMN lastSyncAt INTEGER")
            db.execSQL("ALTER TABLE sticker_packs ADD COLUMN visibility TEXT NOT NULL DEFAULT 'PRIVATE'")
            db.execSQL("ALTER TABLE sticker_packs ADD COLUMN cloudOwnerId TEXT")

            // Add sync columns to stickers
            db.execSQL("ALTER TABLE stickers ADD COLUMN cloudId TEXT")
            db.execSQL("ALTER TABLE stickers ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            db.execSQL("ALTER TABLE stickers ADD COLUMN lastSyncAt INTEGER")
            db.execSQL("ALTER TABLE stickers ADD COLUMN cloudUrl TEXT")

            // Create sync_queue table
            db.execSQL("""
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
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_targetId ON sync_queue(targetId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue(createdAt)")
        }
    }
}
