package com.setiker.app

import android.app.Application
import data.aijob.AiJobManager
import data.aijob.AiJobNotificationSync
import data.aijob.AiNotificationHelper
import data.storage.ForegroundActivityProvider
import data.sync.SyncWorker
import di.appModule
import di.platformModule
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject

class StickerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SetikerRuntimeConfig.install(applicationInfo, getString(R.string.google_web_client_id))
        ForegroundActivityProvider.install(this)

        startKoin {
            androidLogger()
            androidContext(this@StickerApplication)
            modules(platformModule() + appModule)
        }

        SyncWorker(this).schedulePeriodicSync()
        SyncWorker(this).scheduleImmediateSync()

        val notificationHelper: AiNotificationHelper by inject(AiNotificationHelper::class.java)
        notificationHelper.ensureChannel()
        val aiJobManager: AiJobManager by inject(AiJobManager::class.java)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            delay(300.milliseconds)
            val draftRepository: WorkspaceDraftRepository by inject(
                WorkspaceDraftRepository::class.java
            )
            val jobRepository: AiJobRepository by inject(
                AiJobRepository::class.java
            )
            val interrupted = aiJobManager.resumeOnStartup()
            AiJobNotificationSync.applyRecovery(
                notificationHelper = notificationHelper,
                draftRepository = draftRepository,
                jobRepository = jobRepository,
                interrupted = interrupted
            )
        }
    }
}
