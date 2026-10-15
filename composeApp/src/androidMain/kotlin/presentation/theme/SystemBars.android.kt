package presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun ConfigureSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        // Dark icons for light backgrounds and light icons for dark backgrounds.
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Let the app background paint behind the navigation bar instead of the
            // system-provided translucent scrim.
            window.isNavigationBarContrastEnforced = false
        }
    }
}
