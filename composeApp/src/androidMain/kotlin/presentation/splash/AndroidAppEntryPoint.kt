package presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.setiker.app.App
import kotlinx.coroutines.delay
import presentation.navigation.NotificationDeepLink
import presentation.theme.SetikerTheme

/**
 * Android entry point that coordinates the Splash Screen API with the branded Compose splash.
 *
 * Flow:
 * 1. System splash (Splash Screen API theme) stays visible until the first Compose frame is ready.
 * 2. Branded Compose splash reveals attribution text while the system splash exits.
 * 3. Main app content replaces the branded splash after [SplashBranding.MIN_VISIBLE_DURATION_MS].
 */
@Composable
fun AndroidAppEntryPoint(
    keepSystemSplash: () -> Boolean,
    onSystemSplashReadyToDismiss: () -> Unit,
    onAddToWhatsApp: ((String, String) -> Unit)? = null,
    notificationDeepLink: NotificationDeepLink? = null,
    notificationDeepLinkVersion: Int = 0
) {
    var showMainContent by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!showMainContent) {
            SetikerTheme {
                AppBrandedSplashScreen()
            }

            LaunchedEffect(Unit) {
                withFrameNanos { }
                if (keepSystemSplash()) {
                    onSystemSplashReadyToDismiss()
                }
                delay(SplashBranding.MIN_VISIBLE_DURATION_MS)
                showMainContent = true
            }
        }

        if (showMainContent) {
            App(
                onAddToWhatsApp = onAddToWhatsApp,
                notificationDeepLink = notificationDeepLink,
                notificationDeepLinkVersion = notificationDeepLinkVersion
            )
        }
    }
}
