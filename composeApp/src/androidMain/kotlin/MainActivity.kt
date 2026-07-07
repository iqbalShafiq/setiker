package com.setiker.app

import android.content.Intent
import android.Manifest
import com.setiker.app.MainActivityHolder
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import data.aijob.AiNotificationHelper
import presentation.navigation.NotificationDeepLink
import presentation.splash.AndroidAppEntryPoint

class MainActivity : ComponentActivity() {

    private val whatsAppLauncher = WhatsAppStickerLauncher()
    private var notificationDeepLink by mutableStateOf<NotificationDeepLink?>(null)
    private var notificationDeepLinkVersion by mutableIntStateOf(0)
    private var keepSystemSplash by mutableStateOf(true)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        MainActivityHolder.current = this
        applyDeepLinkFromIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        whatsAppLauncher.register(this) { success, error ->
            if (!success) {
                android.util.Log.e("MainActivity", "WhatsApp error: $error")
            }
        }

        splashScreen.setKeepOnScreenCondition { keepSystemSplash }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        setContent {
            AndroidAppEntryPoint(
                keepSystemSplash = { keepSystemSplash },
                onSystemSplashReadyToDismiss = { keepSystemSplash = false },
                onAddToWhatsApp = { packId, packName ->
                    if (WhatsAppStickerLauncher.isWhatsAppInstalled(packageManager)) {
                        whatsAppLauncher.launchAddToWhatsApp(this, packId, packName)
                    }
                },
                notificationDeepLink = notificationDeepLink,
                notificationDeepLinkVersion = notificationDeepLinkVersion
            )
        }
    }

    override fun onDestroy() {
        if (MainActivityHolder.current === this) {
            MainActivityHolder.current = null
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyDeepLinkFromIntent(intent)
        notificationDeepLinkVersion++
    }

    private fun applyDeepLinkFromIntent(intent: Intent?) {
        notificationDeepLink = parseNotificationDeepLink(intent)
    }

    private fun parseNotificationDeepLink(intent: Intent?): NotificationDeepLink? {
        if (intent == null) return null
        if (intent.getBooleanExtra(AiNotificationHelper.EXTRA_OPEN_AI_JOBS, false)) {
            return NotificationDeepLink(
                draftId = null,
                jobId = null,
                openAiJobsOnly = true
            )
        }
        var draftId = intent.getStringExtra(AiNotificationHelper.EXTRA_DRAFT_ID)
        var jobId = intent.getStringExtra(AiNotificationHelper.EXTRA_JOB_ID)
        intent.data?.let { uri ->
            if (uri.scheme == "setiker" && uri.host == "ai" &&
                (uri.path == "/jobs" || uri.lastPathSegment == "jobs")
            ) {
                return NotificationDeepLink(
                    draftId = null,
                    jobId = null,
                    openAiJobsOnly = true
                )
            }
            parseAiDeepLinkUri(uri)?.let { (d, j) ->
                draftId = draftId ?: d
                jobId = jobId ?: j
            }
        }
        if (draftId.isNullOrBlank() && jobId.isNullOrBlank()) return null
        return NotificationDeepLink(
            draftId = draftId?.takeIf { it.isNotBlank() },
            jobId = jobId?.takeIf { it.isNotBlank() }
        )
    }

    private fun parseAiDeepLinkUri(uri: Uri): Pair<String?, String?>? {
        if (uri.scheme != "setiker" || uri.host != "ai") return null
        return when (uri.pathSegments.firstOrNull()) {
            "draft" -> uri.pathSegments.getOrNull(1) to uri.getQueryParameter("jobId")
            "job" -> null to uri.pathSegments.getOrNull(1)
            else -> null
        }
    }
}
