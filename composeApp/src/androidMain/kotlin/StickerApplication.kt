package com.setiker.app

import android.app.Application
import di.appModule
import di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class StickerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@StickerApplication)
            modules(platformModule() + appModule)
        }
    }
}