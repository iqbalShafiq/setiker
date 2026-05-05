package presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = BackgroundWhite,
    primaryContainer = LightGreen,
    onPrimaryContainer = DarkGreen,
    secondary = DarkGreen,
    onSecondary = BackgroundWhite,
    secondaryContainer = LightGreen,
    onSecondaryContainer = DarkGreen,
    tertiary = WarningYellow,
    onTertiary = OnSurfaceDark,
    background = BackgroundWhite,
    onBackground = OnSurfaceDark,
    surface = SurfaceGray,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = OnSurfaceMedium,
    error = ErrorRed,
    onError = BackgroundWhite,
    outline = OnSurfaceMedium.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppGreenLight,
    onPrimary = OnSurfaceDark,
    primaryContainer = DarkGreen,
    onPrimaryContainer = LightGreen,
    secondary = LightGreen,
    onSecondary = OnSurfaceDark,
    secondaryContainer = DarkGreen,
    onSecondaryContainer = LightGreen,
    tertiary = WarningYellow,
    onTertiary = OnSurfaceDark,
    background = OnSurfaceDark,
    onBackground = BackgroundWhite,
    surface = Color(0xFF1F2C34),
    onSurface = BackgroundWhite,
    surfaceVariant = Color(0xFF2A3942),
    onSurfaceVariant = SurfaceGray,
    error = ErrorRed,
    onError = BackgroundWhite,
    outline = SurfaceGray.copy(alpha = 0.5f)
)

@Composable
fun SetikerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
