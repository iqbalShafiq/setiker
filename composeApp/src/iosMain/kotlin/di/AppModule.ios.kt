package di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import data.local.database.StickerDatabase
import data.storage.StickerFileStorage
import data.sync.NetworkMonitor
import data.video.CandidateGridComposer
import data.video.UnsupportedCandidateGridComposer
import data.video.UnsupportedVideoFrameCandidateExtractor
import data.video.VideoFrameCandidateExtractor
import data.util.EmojiPreferences
import data.aijob.AiBackgroundScheduler
import data.util.IosOnDeviceImageProcessor
import data.util.OnDeviceImageProcessor
import data.util.createIOSDataStore
import domain.actions.IosPackActions
import domain.actions.PackActions
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
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single { get<StickerDatabase>().processingHistoryCacheDao() }
    single<StickerFileStorage> { StickerFileStorage() }
    single<EmojiPreferences> { EmojiPreferences() }
    single<OnDeviceImageProcessor> { IosOnDeviceImageProcessor() }
    single<VideoFrameCandidateExtractor> { UnsupportedVideoFrameCandidateExtractor() }
    single<CandidateGridComposer> { UnsupportedCandidateGridComposer() }
    single<PackActions> { IosPackActions() }
    single<DataStore<Preferences>> { createIOSDataStore() }
    single { NetworkMonitor() }
    single { AiBackgroundScheduler() }
}
