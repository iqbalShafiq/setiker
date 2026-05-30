package presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware companions to the Neubrutal palette so every component can render
 * sensibly in both light and dark mode without hard-coding white/black.
 *
 * Goals:
 * - Borders and "thick line" accents stay strong in both themes (dark in light mode,
 *   bright off-white in dark mode).
 * - Card / preview surfaces stay light in light mode and a calm dark in dark mode.
 * - Drop shadows remain hard-edge but soften in dark mode so they don't crush the layout.
 */

@Composable
@ReadOnlyComposable
fun neubrutalBorderColor(): Color =
    if (isSystemInDarkTheme()) NeubrutalWhite else NeubrutalBlack

@Composable
@ReadOnlyComposable
fun neubrutalShadowColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF000000).copy(alpha = 0.85f) else NeubrutalBlack

/** Background of the topmost screen surface. */
@Composable
@ReadOnlyComposable
fun neubrutalScreenBackground(): Color = MaterialTheme.colorScheme.background

/** Background of card-like elements (sticker previews, sheets, dialogs). */
@Composable
@ReadOnlyComposable
fun neubrutalCardSurface(): Color = MaterialTheme.colorScheme.surface

/** App bar panel color. Kept distinct from the screen background for neubrutal layering. */
@Composable
@ReadOnlyComposable
fun neubrutalTopAppBarSurface(): Color =
    if (isSystemInDarkTheme()) AppBarCoralDark else AccentCoralLight

/** Bottom bar panel color. Matches top app bar for a cohesive neubrutal frame. */
@Composable
@ReadOnlyComposable
fun neubrutalBottomAppBarSurface(): Color =
    if (isSystemInDarkTheme()) AppBarCoralDark else AccentCoralLight

/** Strong text / icon color, always readable on the matching surface. */
@Composable
@ReadOnlyComposable
fun neubrutalOnSurface(): Color = MaterialTheme.colorScheme.onSurface

/** Muted text color (hints, captions). */
@Composable
@ReadOnlyComposable
fun neubrutalMutedOnSurface(): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

/** Subtler muted color (placeholder / disabled-ish text). */
@Composable
@ReadOnlyComposable
fun neubrutalSubtleOnSurface(): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

/** Inverse text color (used inside the coral primary buttons / FAB). */
@Composable
@ReadOnlyComposable
fun neubrutalOnPrimary(): Color = MaterialTheme.colorScheme.onPrimary

/**
 * Fill color for the top/left glossy bevel band (solid L-shape inside the border).
 *
 * @param onFilledSurface true for saturated fills (primary buttons, FAB, danger buttons).
 */
@Composable
@ReadOnlyComposable
fun neubrutalGlossyHighlightColor(onFilledSurface: Boolean = false): Color =
    if (isSystemInDarkTheme()) {
        NeubrutalWhite.copy(alpha = if (onFilledSurface) 0.58f else 0.42f)
    } else {
        NeubrutalGlossyHighlightLight
    }
