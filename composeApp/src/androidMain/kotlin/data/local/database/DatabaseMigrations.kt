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

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_drafts (
                    id TEXT NOT NULL PRIMARY KEY,
                    kind TEXT NOT NULL,
                    status TEXT NOT NULL,
                    origin TEXT NOT NULL,
                    originRoute TEXT,
                    packId TEXT,
                    stickerIndex INTEGER,
                    displayTitle TEXT NOT NULL,
                    contextJson TEXT NOT NULL,
                    lastJobId TEXT,
                    lastCompletedJobId TEXT,
                    lastFailedJobId TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_drafts_status ON workspace_drafts(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_drafts_updatedAt ON workspace_drafts(updatedAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_jobs (
                    id TEXT NOT NULL PRIMARY KEY,
                    workspaceDraftId TEXT NOT NULL,
                    type TEXT NOT NULL,
                    status TEXT NOT NULL,
                    origin TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    resultJson TEXT,
                    checkpointJson TEXT,
                    progressStepKey TEXT,
                    progressStepLabel TEXT,
                    progressFraction REAL NOT NULL DEFAULT 0,
                    attemptCount INTEGER NOT NULL DEFAULT 0,
                    attemptGroupId TEXT NOT NULL,
                    parentJobId TEXT,
                    failureKind TEXT NOT NULL DEFAULT 'NONE',
                    failureMessage TEXT,
                    requiresNetwork INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    lastAttemptAt INTEGER,
                    nextRetryAt INTEGER,
                    completedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_status ON ai_jobs(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_workspaceDraftId ON ai_jobs(workspaceDraftId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_attemptGroupId ON ai_jobs(attemptGroupId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_createdAt ON ai_jobs(createdAt)")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE ai_jobs ADD COLUMN quotaReservationId TEXT")
        }
    }
}
