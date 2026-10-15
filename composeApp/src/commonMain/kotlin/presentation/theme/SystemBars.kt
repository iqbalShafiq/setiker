package presentation.theme

import androidx.compose.runtime.Composable

/**
 * Applies the app theme's light/dark setting to the system bar icons.
 *
 * Bar colors are intentionally not set here: the app draws edge-to-edge (see
 * `enableEdgeToEdge` in the entry point activity), so the app background is already
 * painted behind the system bars.
 */
@Composable
expect fun ConfigureSystemBars(darkTheme: Boolean)
