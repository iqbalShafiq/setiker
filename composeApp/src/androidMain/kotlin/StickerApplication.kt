package com.setiker.app

import android.app.Application
import data.storage.ForegroundActivityProvider
import data.aijob.AiJobManager
import data.aijob.AiJobNotificationSync
import data.aijob.AiNotificationHelper
import data.sync.SyncWorker
import org.koin.java.KoinJavaComponent.inject
import di.appModule
import di.platformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class StickerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
            kotlinx.coroutines.delay(300)
            val draftRepository: domain.repository.WorkspaceDraftRepository by inject(
                domain.repository.WorkspaceDraftRepository::class.java
            )
            val jobRepository: domain.repository.AiJobRepository by inject(
                domain.repository.AiJobRepository::class.java
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
