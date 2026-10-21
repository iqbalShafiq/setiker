package com.setiker.app.ui

import android.app.Application

/**
 * A bare Application for the screenshot tests.
 *
 * Robolectric runs the real `Application` of the app under test, and the app's own
 * `StickerApplication` starts Koin and schedules WorkManager jobs. Neither belongs in a test that
 * only draws four components, and both fail in the Robolectric environment.
 */
class ScreenshotTestApplication : Application()
