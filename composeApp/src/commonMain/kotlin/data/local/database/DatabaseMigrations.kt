package data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE stickers ADD COLUMN decorationsJson TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE stickers ADD COLUMN sourceImageFile TEXT")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE sticker_packs ADD COLUMN isAnimated INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE stickers ADD COLUMN isAnimated INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE stickers ADD COLUMN sourceVideoFile TEXT")
            connection.execSQL("ALTER TABLE stickers ADD COLUMN frameDecorationsJson TEXT")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            // Add sync columns to sticker_packs
            connection.execSQL("ALTER TABLE sticker_packs ADD COLUMN cloudId TEXT")
            connection.execSQL("ALTER TABLE sticker_packs ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            connection.execSQL("ALTER TABLE sticker_packs ADD COLUMN lastSyncAt INTEGER")
            connection.execSQL("ALTER TABLE sticker_packs ADD COLUMN visibility TEXT NOT NULL DEFAULT 'PRIVATE'")
            connection.execSQL("ALTER TABLE sticker_packs ADD COLUMN cloudOwnerId TEXT")

            // Add sync columns to stickers
            connection.execSQL("ALTER TABLE stickers ADD COLUMN cloudId TEXT")
            connection.execSQL("ALTER TABLE stickers ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            connection.execSQL("ALTER TABLE stickers ADD COLUMN lastSyncAt INTEGER")
            connection.execSQL("ALTER TABLE stickers ADD COLUMN cloudUrl TEXT")

            // Create sync_queue table
            connection.execSQL("""
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
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue(status)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_targetId ON sync_queue(targetId)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue(createdAt)")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
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
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_drafts_status ON workspace_drafts(status)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_drafts_updatedAt ON workspace_drafts(updatedAt)")

            connection.execSQL(
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
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_status ON ai_jobs(status)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_workspaceDraftId ON ai_jobs(workspaceDraftId)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_attemptGroupId ON ai_jobs(attemptGroupId)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_ai_jobs_createdAt ON ai_jobs(createdAt)")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE ai_jobs ADD COLUMN quotaReservationId TEXT")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS processing_history_cache (
                    id TEXT NOT NULL PRIMARY KEY,
                    type TEXT NOT NULL,
                    outputCount INTEGER NOT NULL,
                    previewUrl TEXT,
                    createdAt INTEGER NOT NULL,
                    syncedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    /**
     * Every Room instance that opens [StickerDatabase] must register this exact list,
     * otherwise a process (e.g. StickerContentProvider for WhatsApp) may open the
     * database with a migrator that cannot handle the current schema version.
     */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
    )
}
