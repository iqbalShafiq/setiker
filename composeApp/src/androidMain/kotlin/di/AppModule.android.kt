package di

import android.content.Context
import androidx.room.Room
import data.local.database.StickerDatabase
import data.storage.StickerFileStorage
import data.util.EmojiPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<StickerDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = StickerDatabase::class.java,
            name = StickerDatabase.DATABASE_NAME
        ).build()
    }

    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single<StickerFileStorage> { StickerFileStorage(androidContext()) }
    single<EmojiPreferences> { EmojiPreferences(androidContext()) }
}