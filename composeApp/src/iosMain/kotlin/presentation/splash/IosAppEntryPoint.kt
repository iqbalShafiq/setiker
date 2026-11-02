package presentation.splash

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.setiker.app.App
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import presentation.theme.SetikerMotion
import presentation.theme.SetikerTheme

/**
 * iOS entry point with the same branded splash as Android.
 *
 * Android coordinates the branded splash with the system Splash Screen API. iOS has no equivalent
 * contract, so the Compose splash is the first surface and crossfades into the app after the same
 * minimum visible duration. Without this the first frame on iOS was the app itself, which made
 * the two platforms feel like different products.
 */
@Composable
fun IosAppEntryPoint() {
    var showMainContent by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = showMainContent,
            animationSpec = tween(
                durationMillis = SetikerMotion.DURATION_SLOW_MS,
                easing = SetikerMotion.EaseOut
            ),
            label = "ios_splash_to_app"
        ) { showApp ->
            if (showApp) {
                App()
            } else {
                SetikerTheme {
                    AppBrandedSplashScreen()
                }
            }
        }

        LaunchedEffect(Unit) {
            delay(SplashBranding.MIN_VISIBLE_DURATION_MS.milliseconds)
            showMainContent = true
        }
    }
}
