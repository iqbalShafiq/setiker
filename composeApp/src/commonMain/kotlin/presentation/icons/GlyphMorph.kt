package presentation.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * A glyph as a fixed *structure* of cubic segments, so two glyphs can be interpolated point by
 * point.
 *
 * This is the difference between a fill and a morph. The fill reveal in `StatefulIcon` keeps one
 * silhouette and wipes ink into it; a morph has to turn one silhouette into another, which is only
 * possible if both sides describe their shape the same way. An `ImageVector` cannot promise that —
 * the number of path commands is whatever the author wrote. A [MorphShape] can, because it is
 * written for the purpose.
 *
 * The structure is: subpaths, each a start point, N cubic curves, and a closed flag. Two shapes can
 * morph if they have the same number of subpaths, the same number of curves in each, and the same
 * closed flags. [GlyphMorph] refuses anything else at construction instead of drawing garbage at
 * 60 fps.
 *
 * [opacity] per subpath exists because a shape that collapses to a point still paints a round join
 * — a pupil shrunk to radius zero would leave a dot behind. The pupil therefore fades as it shrinks.
 */
class MorphShape(val subpaths: List<MorphSubpath>)

class MorphSubpath(
    val start: Offset,
    val curves: List<MorphCubic>,
    val closed: Boolean,
    val opacity: Float = 1f
)

class MorphCubic(val control1: Offset, val control2: Offset, val end: Offset)

/**
 * Two shapes that can be interpolated, plus the guard that makes the promise true.
 *
 * @throws IllegalArgumentException when the two sides are not structurally identical — a loud
 *   failure at construction, rather than a silently deformed glyph on every frame afterwards.
 */
class GlyphMorph(val from: MorphShape, val to: MorphShape) {

    init {
        require(from.subpaths.size == to.subpaths.size) {
            "Morph needs the same subpath count: ${from.subpaths.size} vs ${to.subpaths.size}"
        }
        from.subpaths.forEachIndexed { index, a ->
            val b = to.subpaths[index]
            require(a.curves.size == b.curves.size) {
                "Subpath $index needs the same curve count: ${a.curves.size} vs ${b.curves.size}"
            }
            require(a.closed == b.closed) { "Subpath $index disagrees on closed" }
        }
    }

    val subpathCount: Int get() = from.subpaths.size
}

/** Linear interpolation of a shape pair into [path]; [path] is rewound, not reallocated. */
internal fun MorphSubpath.writeLerpInto(
    target: MorphSubpath,
    t: Float,
    path: Path
) {
    path.rewind()
    path.moveTo(lerp(start.x, target.start.x, t), lerp(start.y, target.start.y, t))
    curves.forEachIndexed { index, curve ->
        val other = target.curves[index]
        path.cubicTo(
            lerp(curve.control1.x, other.control1.x, t),
            lerp(curve.control1.y, other.control1.y, t),
            lerp(curve.control2.x, other.control2.x, t),
            lerp(curve.control2.y, other.control2.y, t),
            lerp(curve.end.x, other.end.x, t),
            lerp(curve.end.y, other.end.y, t)
        )
    }
    if (closed) path.close()
}

internal fun lerp(from: Float, to: Float, fraction: Float): Float =
    from + (to - from) * fraction

/* ============================================
 * The shapes that ship
 * ============================================ */

/**
 * Eye open to eye closed, which is the visibility toggle in Pack Detail.
 *
 * Both sides are two subpaths of two and four cubics: the lid pair and the pupil. Closing works
 * because the lid curves rotate down into a thin crescent while the pupil collapses and fades —
 * the same topology start to finish, so nothing is thrown away mid-flight.
 */
object SetikerMorphs {

    val Eye: GlyphMorph by lazy { GlyphMorph(from = EyeOpen, to = EyeClosed) }

    /**
     * Pause to play, the state-switching case for a transport control.
     *
     * A pause is two bars; a play is a triangle. They cannot morph directly, so both are authored
     * as **two open subpaths of two cubics**: the left bar slides into the triangle's back edge and
     * the right bar bends into the two slanted edges. Same three points per subpath on both sides,
     * so the interpolation is legal — and it reads as one bar opening up rather than as two
     * unrelated pictures crossfading.
     */
    val PlayPause: GlyphMorph by lazy { GlyphMorph(from = Play, to = Pause) }

    /**
     * A busy dot becoming a tick — the "state loaded" case.
     *
     * Both sides are one open subpath of four cubics. The dot is a small loop drawn with round
     * caps, so at the start it reads as a filled pip; the four points then spread into the two legs
     * of the tick.
     */
    val Loaded: GlyphMorph by lazy { GlyphMorph(from = BusyDot, to = DoneTick) }

    /**
     * A magnifier opening into a cross — the search toggle.
     *
     * A ring is a closed shape and a cross is two strokes, so neither side can be handed to the
     * engine as it stands. The ring is authored as an *open* subpath of four quarter arcs whose end
     * lands on its start, and the handle as four short segments; the cross is those same two
     * subpaths folded flat. The ring unfurls into one arm while the handle swings into the other,
     * so the magnifier visibly becomes the cross instead of the two swapping places.
     */
    val SearchClose: GlyphMorph by lazy { GlyphMorph(from = Magnifier, to = Cross) }

    /**
     * A person with a plus becoming a person with a tick — the follow case.
     *
     * The head and the shoulders are identical on both sides; only the mark beside them changes,
     * and that mark is two straight strokes on either side. The plus folds into the tick one arm at
     * a time, which reads as the action being accepted rather than as a new icon arriving.
     */
    val Follow: GlyphMorph by lazy { GlyphMorph(from = PersonAddMark, to = PersonCheckMark) }
}

/** A single cubic is enough for a lash: the control points sit on the line. */
private fun lash(from: Offset, to: Offset, opacity: Float): MorphSubpath {
    val dx = (to.x - from.x) / 3f
    val dy = (to.y - from.y) / 3f
    return MorphSubpath(
        start = from,
        curves = listOf(
            MorphCubic(
                control1 = Offset(from.x + dx, from.y + dy),
                control2 = Offset(from.x + dx * 2f, from.y + dy * 2f),
                end = to
            )
        ),
        closed = false,
        opacity = opacity
    )
}

private val hidden = Offset(12f, 12f)
private val LashLeftOpen = lash(hidden, hidden, 0f)
private val LashMiddleOpen = lash(hidden, hidden, 0f)
private val LashRightOpen = lash(hidden, hidden, 0f)

private val LashLeftClosed = lash(Offset(6.6f, 13.5f), Offset(5.4f, 15.4f), 1f)
private val LashMiddleClosed = lash(Offset(12f, 14.1f), Offset(12f, 16.2f), 1f)
private val LashRightClosed = lash(Offset(17.4f, 13.5f), Offset(18.6f, 15.4f), 1f)

private val EyeOpen = MorphShape(
    subpaths = listOf(
        MorphSubpath(
            start = Offset(3.6f, 12f),
            curves = listOf(
                MorphCubic(Offset(7.2f, 7.2f), Offset(16.8f, 7.2f), Offset(20.4f, 12f)),
                MorphCubic(Offset(16.8f, 16.8f), Offset(7.2f, 16.8f), Offset(3.6f, 12f))
            ),
            closed = true
        ),
        MorphSubpath(
            start = Offset(15.1f, 12f),
            curves = listOf(
                MorphCubic(Offset(15.1f, 13.712f), Offset(13.712f, 15.1f), Offset(12f, 15.1f)),
                MorphCubic(Offset(10.288f, 15.1f), Offset(8.9f, 13.712f), Offset(8.9f, 12f)),
                MorphCubic(Offset(8.9f, 10.288f), Offset(10.288f, 8.9f), Offset(12f, 8.9f)),
                MorphCubic(Offset(13.712f, 8.9f), Offset(15.1f, 10.288f), Offset(15.1f, 12f))
            ),
            closed = true,
            opacity = 1f
        ),
        // The lashes exist in both states because a morph cannot add geometry; in the open state
        // they are collapsed onto the lid and fully transparent.
        LashLeftOpen,
        LashMiddleOpen,
        LashRightOpen
    )
)

private val EyeClosed = MorphShape(
    subpaths = listOf(
        MorphSubpath(
            start = Offset(3.6f, 12f),
            curves = listOf(
                MorphCubic(Offset(7.2f, 14.4f), Offset(16.8f, 14.4f), Offset(20.4f, 12f)),
                MorphCubic(Offset(16.8f, 13.3f), Offset(7.2f, 13.3f), Offset(3.6f, 12f))
            ),
            closed = true
        ),
        MorphSubpath(
            start = Offset(12f, 12f),
            curves = listOf(
                MorphCubic(Offset(12f, 12f), Offset(12f, 12f), Offset(12f, 12f)),
                MorphCubic(Offset(12f, 12f), Offset(12f, 12f), Offset(12f, 12f)),
                MorphCubic(Offset(12f, 12f), Offset(12f, 12f), Offset(12f, 12f)),
                MorphCubic(Offset(12f, 12f), Offset(12f, 12f), Offset(12f, 12f))
            ),
            closed = true,
            opacity = 0f
        ),
        LashLeftClosed,
        LashMiddleClosed,
        LashRightClosed
    )
)

/* ============================================
 * Transport: pause to play
 * ============================================ */

/** Two bars, each split in half so the subpath carries three points. */
private fun bar(x: Float, opacity: Float = 1f) = MorphSubpath(
    start = Offset(x, 5.5f),
    curves = listOf(
        MorphCubic(Offset(x, 7.67f), Offset(x, 9.83f), Offset(x, 12f)),
        MorphCubic(Offset(x, 14.17f), Offset(x, 16.33f), Offset(x, 18.5f))
    ),
    closed = false,
    opacity = opacity
)

private val Pause = MorphShape(
    subpaths = listOf(bar(9f), bar(15f))
)

private val Play = MorphShape(
    subpaths = listOf(
        // Subpath 0 is the *left* bar, so it becomes the back edge - the bar that barely moves.
        MorphSubpath(
            start = Offset(7.2f, 5f),
            curves = listOf(
                MorphCubic(Offset(7.2f, 7.33f), Offset(7.2f, 9.67f), Offset(7.2f, 12f)),
                MorphCubic(Offset(7.2f, 14.33f), Offset(7.2f, 16.67f), Offset(7.2f, 19f))
            ),
            closed = false
        ),
        // The right bar opens into the two slanted edges of the triangle.
        MorphSubpath(
            start = Offset(7.2f, 5f),
            curves = listOf(
                MorphCubic(Offset(10.5f, 7.4f), Offset(13.8f, 9.7f), Offset(17f, 12f)),
                MorphCubic(Offset(13.8f, 14.3f), Offset(10.5f, 16.6f), Offset(7.2f, 19f))
            ),
            closed = false
        )
    )
)

/* ============================================
 * Loaded: busy dot to tick
 * ============================================ */

/**
 * The busy pip, authored as five points clustered inside a 2 x 1.6 unit box.
 *
 * It used to be a proper ring, and the ring's points travel *around* a loop while the tick's travel
 * *along* a path — so halfway through, the two orderings crossed and the glyph read as a hook.
 * Clustering the points and ordering them left-to-right, matching the tick's own left-to-right
 * order, turns the transition into the pip stretching sideways into the check.
 */
private val BusyDot = MorphShape(
    subpaths = listOf(
        MorphSubpath(
            start = Offset(10.5f, 12.3f),
            curves = listOf(
                MorphCubic(Offset(10.633f, 12.667f), Offset(10.767f, 13.033f), Offset(10.9f, 13.4f)),
                MorphCubic(Offset(11.133f, 13.467f), Offset(11.367f, 13.533f), Offset(11.6f, 13.6f)),
                MorphCubic(Offset(11.833f, 13.333f), Offset(12.067f, 13.067f), Offset(12.3f, 12.8f)),
                MorphCubic(Offset(12.433f, 12.567f), Offset(12.567f, 12.333f), Offset(12.7f, 12.1f))
            ),
            closed = false
        )
    )
)

private val DoneTick = MorphShape(
    subpaths = listOf(
        MorphSubpath(
            start = Offset(6.4f, 12.6f),
            curves = listOf(
                MorphCubic(Offset(7.133f, 13.2f), Offset(7.867f, 13.8f), Offset(8.6f, 14.4f)),
                MorphCubic(Offset(9.4f, 15.133f), Offset(10.2f, 15.867f), Offset(11f, 16.6f)),
                MorphCubic(Offset(12.2f, 15f), Offset(13.4f, 13.4f), Offset(14.6f, 11.8f)),
                MorphCubic(Offset(15.6f, 10.333f), Offset(16.6f, 8.867f), Offset(17.6f, 7.4f))
            ),
            closed = false
        )
    )
)
/**
 * A straight line as [segments] collinear cubics.
 *
 * A line has no curve count of its own, which is what makes it useless for a morph: the engine
 * compares counts exactly. Splitting it into equal cubics gives it a count the caller can match
 * against whatever curve it has to meet.
 */
private fun straight(from: Offset, to: Offset, segments: Int): MorphSubpath {
    val curves = ArrayList<MorphCubic>(segments)
    for (index in 0 until segments) {
        val a = Offset(
            x = from.x + (to.x - from.x) * index.toFloat() / segments,
            y = from.y + (to.y - from.y) * index.toFloat() / segments
        )
        val b = Offset(
            x = from.x + (to.x - from.x) * (index + 1).toFloat() / segments,
            y = from.y + (to.y - from.y) * (index + 1).toFloat() / segments
        )
        val step = (b - a) / 3f
        curves += MorphCubic(control1 = a + step, control2 = a + step * 2f, end = b)
    }
    return MorphSubpath(start = from, curves = curves, closed = false)
}

/**
 * A ring as four quarter arcs that ends where it starts.
 *
 * Authoring it open rather than closed is the whole point: a closed subpath can only morph into
 * another closed subpath, and a cross is open. Round caps hide the seam the open flag leaves.
 */
private fun ring(center: Offset, radius: Float, closed: Boolean = false): MorphSubpath {
    val k = radius * 0.5523f
    return MorphSubpath(
        start = Offset(center.x + radius, center.y),
        curves = listOf(
            MorphCubic(
                Offset(center.x + radius, center.y + k),
                Offset(center.x + k, center.y + radius),
                Offset(center.x, center.y + radius)
            ),
            MorphCubic(
                Offset(center.x - k, center.y + radius),
                Offset(center.x - radius, center.y + k),
                Offset(center.x - radius, center.y)
            ),
            MorphCubic(
                Offset(center.x - radius, center.y - k),
                Offset(center.x - k, center.y - radius),
                Offset(center.x, center.y - radius)
            ),
            MorphCubic(
                Offset(center.x + k, center.y - radius),
                Offset(center.x + radius, center.y - k),
                Offset(center.x + radius, center.y)
            )
        ),
        closed = closed
    )
}

private val Magnifier = MorphShape(
    subpaths = listOf(
        ring(center = Offset(10.4f, 10.4f), radius = 6.5f),
        straight(from = Offset(15.3f, 15.3f), to = Offset(19.7f, 19.7f), segments = 4)
    )
)

private val Cross = MorphShape(
    subpaths = listOf(
        straight(from = Offset(6f, 6f), to = Offset(18f, 18f), segments = 4),
        straight(from = Offset(18f, 6f), to = Offset(6f, 18f), segments = 4)
    )
)

/** The shoulders arc of the person glyph: identical on both sides of the follow morph. */
private val PersonShoulders = MorphSubpath(
    start = Offset(3.2f, 19.4f),
    curves = listOf(
        MorphCubic(
            control1 = Offset(4.7f, 15.1f),
            control2 = Offset(14.1f, 15.1f),
            end = Offset(15.6f, 19.4f)
        )
    ),
    closed = false
)

private val PersonHead = ring(center = Offset(9.4f, 8.2f), radius = 3.3f, closed = true)

private val PersonAddMark = MorphShape(
    subpaths = listOf(
        PersonHead,
        PersonShoulders,
        straight(from = Offset(16.8f, 9.3f), to = Offset(21.2f, 9.3f), segments = 1),
        straight(from = Offset(19f, 7.1f), to = Offset(19f, 11.5f), segments = 1)
    )
)

private val PersonCheckMark = MorphShape(
    subpaths = listOf(
        PersonHead,
        PersonShoulders,
        straight(from = Offset(16.8f, 9.1f), to = Offset(18.6f, 11.3f), segments = 1),
        straight(from = Offset(18.6f, 11.3f), to = Offset(21.6f, 6.8f), segments = 1)
    )
)