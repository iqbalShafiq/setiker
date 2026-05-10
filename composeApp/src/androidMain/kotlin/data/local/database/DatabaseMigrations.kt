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
}
