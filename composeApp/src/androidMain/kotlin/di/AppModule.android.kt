package di

import android.content.Context
import androidx.room.Room
import data.local.database.DatabaseMigrations
import data.local.database.StickerDatabase
import data.storage.StickerFileStorage
import data.util.EmojiPreferences
import domain.actions.AndroidPackActions
import domain.actions.PackActions
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<StickerDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = StickerDatabase::class.java,
            name = StickerDatabase.DATABASE_NAME
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .addMigrations(DatabaseMigrations.MIGRATION_2_3)
            .addMigrations(DatabaseMigrations.MIGRATION_3_4)
            .build()
    }

    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single<StickerFileStorage> { StickerFileStorage(androidContext()) }
    single<EmojiPreferences> { EmojiPreferences(androidContext()) }
    single<PackActions> { AndroidPackActions(androidContext(), get(), get()) }
}