package presentation.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import presentation.theme.IconGridSize
import presentation.theme.IconStrokeWidth

/**
 * The glyphs that are actually Setiker's, drawn to the contract in `theme/Iconography.kt`.
 *
 * Scope is deliberate: only the glyphs that appear in a bar, carry a product meaning, or sit in a
 * cluster where a Material glyph would visibly disagree with its neighbour. Back/forward, chevrons,
 * keyboard arrows, cloud states and legal icons stay on Material — nobody reads them and a second
 * art direction there would be decoration for its own sake.
 *
 * Every glyph is stroked at [IconStrokeWidth] with round caps and joins, on the 24 unit
 * [IconGridSize] grid, inside a 20 unit live area. Two glyphs break the stroke rule on purpose and
 * the reason is in the comment above each: [Sparkle] and the needle in [Explore].
 */
object SetikerIcons {

    /** Primary create action. Bars, FAB, empty grids. */
    val Add: ImageVector by lazy {
        strokeIcon("Setiker.Add") { stroke("M12 4 V20 M4 12 H20") }
    }

    /** Dismiss. Paired with [Add] in the close/open rhythm. */
    val Close: ImageVector by lazy {
        strokeIcon("Setiker.Close") { stroke("M6 6 L18 18 M18 6 L6 18") }
    }

    /** Confirm and save — the app's most repeated glyph. */
    val Check: ImageVector by lazy {
        strokeIcon("Setiker.Check") { stroke("M4.2 12.6 L9.6 18 L19.8 6.2") }
    }

    /** Back navigation. Auto-mirrors, like the Material glyph it replaces. */
    val Back: ImageVector by lazy {
        strokeIcon("Setiker.Back", autoMirror = true) {
            stroke("M20 12 H4.4 M11 5.4 L4.4 12 L11 18.6")
        }
    }

    /** Row disclosure. Auto-mirrors. */
    val Chevron: ImageVector by lazy {
        strokeIcon("Setiker.Chevron", autoMirror = true) {
            stroke("M9 4.6 L16.4 12 L9 19.4")
        }
    }

    /** Search. The handle is drawn rather than the Material lens ring so the weight matches. */
    val Search: ImageVector by lazy {
        strokeIcon("Setiker.Search") {
            stroke("M16.9 10.4 a6.5 6.5 0 1 1 -13 0 a6.5 6.5 0 1 1 13 0 M15.3 15.3 L19.7 19.7")
        }
    }

    /**
     * Explore. The needle is filled: an outlined needle collapses into a grey smudge at 22 dp, and
     * a compass without a solid needle stops reading as a compass.
     */
    val Explore: ImageVector by lazy {
        strokeIcon("Setiker.Explore") {
            stroke("M20.4 12 a8.4 8.4 0 1 1 -16.8 0 a8.4 8.4 0 1 1 16.8 0")
            fill("M15.54 8.46 L13.41 13.41 L8.46 15.54 L10.59 10.59 Z")
        }
    }

    /** Account. Narrow shoulders: at 22 dp a wide arc reads as a coat hanger, not a person. */
    val Person: ImageVector by lazy {
        strokeIcon("Setiker.Person") {
            stroke(
                "M15.4 7.9 a3.4 3.4 0 1 1 -6.8 0 a3.4 3.4 0 1 1 6.8 0 " +
                    "M6 20.2 C6 16.4 8.6 13.9 12 13.9 C15.4 13.9 18 16.4 18 20.2"
            )
        }
    }

    /**
     * Sync and retry. One glyph serves both: they are the same intent ("do it again") and the app
     * was showing `Refresh` in the bar while calling it sync.
     *
     * The arrowhead is an open chevron, not a filled triangle. A triangle small enough to fit the
     * 0.5 unit gap collapsed into a blob at 22 dp and the whole glyph read as a broken ring; two
     * round-capped segments stay crisp at every size the app uses.
     */
    val Sync: ImageVector by lazy {
        strokeIcon("Setiker.Sync") {
            stroke("M18.9 12 A6.9 6.9 0 1 1 15.45 6.02 M14.3 8.02 L17.7 7.32 L16.6 4.03")
        }
    }

    /**
     * Video to sticker pack.
     *
     * A clapperboard was drawn and discarded twice: at 22 dp its lid and body separate into what
     * reads as a shopping basket, and with the stripe it reads as a toolbox. A play mark inside a
     * frame is the one video glyph that survives the size.
     */
    val Movie: ImageVector by lazy {
        strokeIcon("Setiker.Movie") {
            stroke(
                "M6.4 3.6 H17.6 A2.8 2.8 0 0 1 20.4 6.4 V17.6 A2.8 2.8 0 0 1 17.6 20.4 " +
                    "H6.4 A2.8 2.8 0 0 1 3.6 17.6 V6.4 A2.8 2.8 0 0 1 6.4 3.6 Z"
            )
            fill("M10.2 8.4 L16.2 12 L10.2 15.6 Z")
        }
    }

    /**
     * The AI accent, and the one fully filled glyph in the set.
     *
     * An outlined four-pointed star is a hairline tangle at 22 dp — the counters are thinner than
     * the stroke. Filled, it reads as a spark from across the room, and because it is the only
     * filled glyph it becomes the thing the eye finds first in a bar, which is what an accent is
     * for.
     */
    val Sparkle: ImageVector by lazy {
        strokeIcon("Setiker.Sparkle") {
            fill(
                "M10.4 2 C11.1 6.6 14.2 9.7 18.8 10.4 C14.2 11.1 11.1 14.2 10.4 18.8 " +
                    "C9.7 14.2 6.6 11.1 2 10.4 C6.6 9.7 9.7 6.6 10.4 2 Z"
            )
            fill(
                "M18.6 14.9 C18.9 16.9 20.1 18.1 22.1 18.4 C20.1 18.7 18.9 19.9 18.6 21.9 " +
                    "C18.3 19.9 17.1 18.7 15.1 18.4 C17.1 18.1 18.3 16.9 18.6 14.9 Z"
            )
        }
    }

    /** Image decoration in the editor. */
    val Image: ImageVector by lazy {
        strokeIcon("Setiker.Image") {
            stroke(
                "M5.8 4.6 H18.2 A2.4 2.4 0 0 1 20.6 7 V17 A2.4 2.4 0 0 1 18.2 19.4 " +
                    "H5.8 A2.4 2.4 0 0 1 3.4 17 V7 A2.4 2.4 0 0 1 5.8 4.6 Z"
            )
            fill("M10 9.4 a1.45 1.45 0 1 1 -2.9 0 a1.45 1.45 0 1 1 2.9 0")
            stroke("M5.4 17.6 L9.8 12.6 L14.2 17.6")
        }
    }

    /**
     * Emoji tag. The eyes are 0.6 unit segments whose round caps become the dots, so the face has
     * one stroke weight instead of two.
     */
    val TagFaces: ImageVector by lazy {
        strokeIcon("Setiker.TagFaces") {
            stroke(
                "M20.4 12 a8.4 8.4 0 1 1 -16.8 0 a8.4 8.4 0 1 1 16.8 0 " +
                    "M9.2 9.3 V9.9 M14.8 9.3 V9.9 " +
                    "M8.4 13.6 A5 5 0 0 0 15.6 13.6"
            )
        }
    }

    /** Sort. Three descending rules, right-aligned so the menu reads as ordered. */
    val Sort: ImageVector by lazy {
        strokeIcon("Setiker.Sort") { stroke("M4 7 H20 M4 12 H14.4 M4 17 H9") }
    }

    /** Edit. */
    val Edit: ImageVector by lazy {
        strokeIcon("Setiker.Edit") {
            stroke("M4.4 19.6 L5.2 15.5 L15.6 5.1 A2.75 2.75 0 0 1 19.4 8.9 L9 19.3 Z")
        }
    }

    /** Destructive. */
    val Delete: ImageVector by lazy {
        strokeIcon("Setiker.Delete") {
            stroke(
                "M4.2 6.7 H19.8 M9.7 6.7 V4.5 H14.3 V6.7 " +
                    "M6.4 6.7 L7.3 18.9 A2.1 2.1 0 0 0 9.4 20.7 H14.6 A2.1 2.1 0 0 0 16.7 18.9 L17.6 6.7 " +
                    "M10.5 10.4 V17 M13.5 10.4 V17"
            )
        }
    }

    /** Like, unfilled. The border layer of [HeartMorph]. */
    val Heart: ImageVector by lazy {
        strokeIcon("Setiker.Heart") { stroke(HeartPath) }
    }

    /**
     * Like, filled. Deliberately *fill only*, with no stroke.
     *
     * The outline is drawn on top by the morph, so a stroked solid would double the border to 5 dp
     * at the moment the fill completes.
     */
    val HeartSolid: ImageVector by lazy {
        strokeIcon("Setiker.HeartSolid") { fill(HeartPath) }
    }

    /** The first stateful pair: the like toggle. */
    val HeartMorph: MorphIcon by lazy { MorphIcon(outline = Heart, solid = HeartSolid) }

    /** Save, unfilled. The border layer of [BookmarkMorph]. */
    val Bookmark: ImageVector by lazy {
        strokeIcon("Setiker.Bookmark") { stroke(BookmarkPath) }
    }

    /** Save, filled. Fill only, for the same reason as [HeartSolid]. */
    val BookmarkSolid: ImageVector by lazy {
        strokeIcon("Setiker.BookmarkSolid") { fill(BookmarkPath) }
    }

    /** The save toggle. */
    val BookmarkMorph: MorphIcon by lazy {
        MorphIcon(outline = Bookmark, solid = BookmarkSolid)
    }
}

/**
 * One bookmark, two drawings.
 *
 * The notch sits at y = 15.6 rather than deeper: the filled version fills the notch in, and a deep
 * V turns the solid shape into a pennant instead of a ribbon.
 */
private const val BookmarkPath =
    "M6.6 3.8 H17.4 A1.6 1.6 0 0 1 19 5.4 V20.4 L12 15.6 L5 20.4 V5.4 A1.6 1.6 0 0 1 6.6 3.8 Z"

/**
 * One heart, two drawings.
 *
 * The dip between the lobes sits at y = 6.4 rather than the 5.8 a looser curve would give: at the
 * 22 dp bar size a shallower dip reads as a shield, not a heart.
 */
private const val HeartPath =
    "M12 20.6 C12 20.6 3.3 14.6 3.3 9.2 C3.3 6.1 5.7 3.8 8.4 3.8 " +
        "C10.2 3.8 11.4 5 12 6.4 C12.6 5 13.8 3.8 15.6 3.8 " +
        "C18.3 3.8 20.7 6.1 20.7 9.2 C20.7 14.6 12 20.6 12 20.6 Z"

private fun strokeIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = IconGridSize,
    viewportHeight = IconGridSize,
    autoMirror = autoMirror
).apply(block).build()

/**
 * Placement rule: the vector is painted with black so the caller's tint has a single source. An
 * `ImageVector` is recoloured wholesale by `Icon`, so `SolidColor.ColorBlack` here is not a colour
 * decision — it is the absence of one.
 */
private fun ImageVector.Builder.stroke(pathData: String) {
    addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = IconStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )
}

private fun ImageVector.Builder.fill(pathData: String) {
    addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black)
    )
}
