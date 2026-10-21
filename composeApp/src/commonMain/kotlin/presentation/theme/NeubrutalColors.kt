package presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

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
    if (isSystemInDarkTheme()) {
        // A *black* hard shadow on a near-black page is almost invisible: measured on device the
        // shadow/page delta collapsed from ~219 (light) to ~19 (dark), which erased the whole
        // neubrutal "raised card" language. Dark mode therefore uses a light offset instead — the
        // same hard-edged rectangle, just inverted, so the depth cue survives.
        Color(0xFFFFFFFF).copy(alpha = 0.16f).compositeOver(Color(0xFF161616))
    } else {
        NeubrutalBlack
    }

/**
 * Opaque "disabled" fill for a control.
 *
 * Every neubrutal control paints an opaque hard shadow *behind* itself. A translucent fill would
 * therefore composite with that black shadow and render as a dark, muddy colour that reads as
 * *enabled* instead of disabled. Disabled fills have to be pre-composited against the surface.
 */
@Composable
@ReadOnlyComposable
fun neubrutalDisabledFill(
    base: Color,
    surface: Color = neubrutalCardSurface(),
    strength: Float = 0.4f
): Color = base.copy(alpha = strength).compositeOver(surface)

/** Muted border colour for disabled controls. */
@Composable
@ReadOnlyComposable
fun neubrutalDisabledBorder(): Color =
    neubrutalBorderColor().copy(alpha = 0.45f).compositeOver(neubrutalCardSurface())

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
 * Transparency checkerboard painted behind sticker previews. Both squares are theme surfaces so a
 * dark-mode preview does not flash a white board.
 */
@Composable
@ReadOnlyComposable
fun neubrutalCheckerSquare(): Color =
    if (isSystemInDarkTheme()) Color(0xFF4A4A4A) else Color(0xFFE6E6E6)

/**
 * Fill color for the top/left glossy bevel band (solid L-shape inside the border).
 *
 * @param onFilledSurface true for saturated fills (primary buttons, FAB, danger buttons).
 */
@Composable
@ReadOnlyComposable
fun neubrutalGlossyHighlightColor(onFilledSurface: Boolean = false): Color =
    if (isSystemInDarkTheme()) {
        if (onFilledSurface) {
            NeubrutalGlossyHighlightDarkFilled.copy(alpha = 0.88f)
        } else {
            NeubrutalGlossyHighlightDark.copy(alpha = 0.74f)
        }
    } else {
        NeubrutalGlossyHighlightLight
    }
