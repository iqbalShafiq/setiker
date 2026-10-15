package di

import com.setiker.app.MainActivityHolder
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import data.local.database.DatabaseMigrations
import data.local.database.StickerDatabase
import data.storage.StickerFileStorage
import data.sync.NetworkMonitor
import data.video.AndroidCandidateGridComposer
import data.video.AndroidVideoFrameCandidateExtractor
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
import data.util.EmojiPreferences
import data.aijob.AiBackgroundScheduler
import data.aijob.AiNotificationHelper
import data.util.AndroidOnDeviceImageProcessor
import data.util.OnDeviceImageProcessor
import domain.actions.AndroidPackActions
import domain.actions.PackActions
import data.billing.PlatformBillingStore
import data.auth.AndroidGoogleSignInGateway
import data.auth.GoogleSignInGateway
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "setiker_prefs")

actual fun platformModule(): Module = module {
    single<StickerDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = StickerDatabase::class.java,
            name = StickerDatabase.DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
    }

    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single { get<StickerDatabase>().aiJobDao() }
    single { get<StickerDatabase>().workspaceDraftDao() }
    single { get<StickerDatabase>().processingHistoryCacheDao() }
    single<StickerFileStorage> { StickerFileStorage(androidContext()) }
    single<EmojiPreferences> { EmojiPreferences(androidContext()) }
    single<OnDeviceImageProcessor> { AndroidOnDeviceImageProcessor(androidContext()) }
    single<VideoFrameCandidateExtractor> { AndroidVideoFrameCandidateExtractor(fileStorage = get()) }
    single<CandidateGridComposer> { AndroidCandidateGridComposer(context = androidContext()) }
    single<PackActions> { AndroidPackActions(androidContext(), get(), get()) }
    single<DataStore<Preferences>> { androidContext().dataStore }
    single { NetworkMonitor(androidContext()) }
    single { AiBackgroundScheduler(androidContext()) }
    single { AiNotificationHelper(androidContext()) }
    single { PlatformBillingStore { MainActivityHolder.current } }
    single<GoogleSignInGateway> { AndroidGoogleSignInGateway() }
    single<data.auth.AppleSignInGateway> { data.auth.UnavailableAppleSignInGateway() }
}
