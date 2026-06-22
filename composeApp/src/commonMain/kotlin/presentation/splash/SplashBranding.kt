package presentation.splash

import androidx.compose.ui.graphics.Color
import presentation.theme.NeubrutalBg

/**
 * Shared splash branding tokens used by the Android Splash Screen theme and Compose UI
 * so the system splash and in-app branded splash stay visually aligned.
 */
object SplashBranding {
    val backgroundLight: Color = NeubrutalBg
    val backgroundDark: Color = Color(0xFF161616)

    /** Minimum time the branded Compose splash stays visible after the system splash exits. */
    const val MIN_VISIBLE_DURATION_MS = 700L
}
