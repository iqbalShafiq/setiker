package presentation.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Two drawings of the same silhouette: the resting outline and the filled body.
 *
 * Both must be the *same path data*. [outline] is stroked and supplies the border; [solid] is
 * filled and supplies the ink that the fill animation reveals underneath it. Keeping the border on
 * its own layer is what lets a filled Setiker icon stay neubrutal — a coral body inside a 2.5 dp
 * black outline — instead of becoming a soft shape with a coloured edge.
 *
 * @param outline stroked glyph, drawn last and always fully visible.
 * @param solid filled glyph, clipped by the reveal.
 */
class MorphIcon(
    val outline: ImageVector,
    val solid: ImageVector
)
