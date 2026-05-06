package presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AccentCoral,
    onPrimary = NeubrutalWhite,
    primaryContainer = AccentCoralLight,
    onPrimaryContainer = NeubrutalDark,
    secondary = PastelMint,
    onSecondary = NeubrutalDark,
    secondaryContainer = PastelMint,
    onSecondaryContainer = NeubrutalDark,
    tertiary = WarningYellow,
    onTertiary = NeubrutalDark,
    background = NeubrutalBg,
    onBackground = NeubrutalDark,
    surface = NeubrutalWhite,
    onSurface = NeubrutalDark,
    surfaceVariant = NeubrutalBg,
    onSurfaceVariant = NeubrutalGray,
    error = ErrorRed,
    onError = NeubrutalWhite,
    outline = NeubrutalBlack
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCoral,
    onPrimary = NeubrutalDark,
    primaryContainer = AccentCoral.copy(alpha = 0.3f),
    onPrimaryContainer = AccentCoralLight,
    secondary = PastelMint,
    onSecondary = NeubrutalDark,
    secondaryContainer = PastelMint.copy(alpha = 0.3f),
    onSecondaryContainer = PastelMint,
    tertiary = WarningYellow,
    onTertiary = NeubrutalDark,
    background = NeubrutalDark,
    onBackground = NeubrutalWhite,
    surface = Color(0xFF3A3A3A),
    onSurface = NeubrutalWhite,
    surfaceVariant = Color(0xFF454545),
    onSurfaceVariant = NeubrutalGray,
    error = ErrorRed,
    onError = NeubrutalWhite,
    outline = NeubrutalWhite.copy(alpha = 0.5f)
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
