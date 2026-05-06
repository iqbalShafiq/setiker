package di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import data.local.database.StickerDatabase
import data.storage.StickerFileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun platformModule(): Module = module {
    single<StickerDatabase> {
        val documentsDir = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull()?.path ?: ""
        val dbFile = "$documentsDir/${StickerDatabase.DATABASE_NAME}"

        Room.databaseBuilder<StickerDatabase>(
            name = dbFile
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single<StickerFileStorage> { StickerFileStorage() }
}