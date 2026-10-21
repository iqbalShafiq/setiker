package presentation.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * A flat colour [Painter], used as the Coil placeholder and error painter.
 *
 * Coil 3 does not ship a colour painter, and image placeholders matter here: without one, a sticker
 * cover renders as a blank square until the bitmap arrives, which reads as "broken" rather than
 * "loading".
 */
class SolidColorPainter(private val color: Color) : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(color = color)
    }
}
