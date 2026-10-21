package com.setiker.app

import android.content.pm.ApplicationInfo

/**
 * Runtime source for the values that used to be provided by the app module's `BuildConfig`.
 *
 * The Android KMP library plugin produces a single variant and generates no `BuildConfig`,
 * so these values are installed from the running application at startup.
 *
 * [StickerApplication.onCreate] installs them as its first statement, before Koin, networking
 * or UI code can read them, so no call site needs to handle an "uninstalled" state.
 */
object SetikerRuntimeConfig {

    /** Replacement for the old `BuildConfig.DEBUG`. */
    @Volatile
    var isDebuggable: Boolean = false
        private set

    /**
     * Google OAuth web client id. Empty when `GOOGLE_WEB_CLIENT_ID` is absent from
     * `local.properties`; in that case Google Sign-In is reported as unavailable.
     */
    @Volatile
    var googleWebClientId: String = ""
        private set

    fun install(applicationInfo: ApplicationInfo, googleWebClientId: String) {
        isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        this.googleWebClientId = googleWebClientId
    }
}
