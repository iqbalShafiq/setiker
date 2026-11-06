package presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================
 * Iconography contract
 * ============================================
 *
 * The chrome was de-Materialised one control at a time (buttons, chips, sliders, sheets, progress),
 * but the ink inside those controls was still Google's. Every icon in the app was a Material glyph:
 * a 2.0 unit stroke on a 24 unit grid, which renders as 1.83 dp at the 22 dp bar size — thinner than
 * the 2.5 dp border wrapped around it. The frame was confident and the content was underfed.
 *
 * These are the rules a Setiker glyph obeys, and they exist because of how a glyph looks *next to a
 * border*, not because they were convenient:
 *
 * 1. Grid 24, live area inset 2 — a stroke may not come within 2 units of the edge, or its round
 *    cap collides with the border it sits inside.
 * 2. Stroke [IconStrokeWidth], equal to [NeubrutalBorderWidth]. This is the single number that puts
 *    a glyph *in* the frame instead of floating inside it.
 * 3. Round caps and round joins. A square cap reads as a different tool; this app is hand-made.
 * 4. Fill is a *state*, not a style. The outline-to-filled pair (bookmark, heart) means "not saved /
 *    saved". Mixing a solid glyph next to an outline glyph of the same rank is the one thing this
 *    file exists to prevent. Chrome at [AppIconSize.Bar] is stroked; a solid glyph is allowed only
 *    as an accent (the AI sparkle) or at [AppIconSize.Inline], where a 2.5 stroke at 16 dp would
 *    fill the counters and turn to mush.
 */

/** The viewport every glyph is drawn on. Material uses the same grid, so the two can coexist. */
const val IconGridSize = 24f

/**
 * Stroke width in viewport units.
 *
 * Deliberately equal to [NeubrutalBorderWidth] so a glyph's weight answers the frame's weight.
 * Material's 2.0 reads as a hairline inside a 2.5 dp border.
 */
const val IconStrokeWidth = 2.5f

/**
 * The optical size ladder. There are four steps and no more: a glyph is sized by the control it
 * sits in, not by the screen it happens to be on.
 *
 * The previous code wrote `Modifier.size(22.dp)` at every call site and left the FAB glyph at
 * Compose's implicit 24 dp, so the app's most important icon had no size decision at all.
 */
object AppIconSize {

    /** Inside chips, dense rows and small badges. */
    val Inline: Dp = 16.dp

    /** Leading icon of a text field, trailing icon of a list row. */
    val Compact: Dp = 20.dp

    /** Top bar and bottom bar buttons — the default. */
    val Bar: Dp = 22.dp

    /** The single primary action of a screen. One step louder than a bar icon. */
    val Fab: Dp = 26.dp
}
