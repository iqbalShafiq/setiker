package presentation.components

import androidx.compose.ui.unit.Constraints
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The empty state is rendered inside `AnimatedContent` on several screens, and an
 * `AnimatedContent` measures its content with **infinite** height constraints. A
 * `verticalScroll` in that context crashes the app with "Vertically scrollable component was
 * measured with an infinity maximum height constraints".
 */
class EmptyStateScrollModeTest {

    @Test
    fun `a bounded height scrolls`() {
        assertEquals(EmptyStateScrollMode.SCROLLABLE, emptyStateScrollMode(1080))
    }

    @Test
    fun `an unbounded height does not scroll`() {
        assertEquals(EmptyStateScrollMode.WRAP_CONTENT, emptyStateScrollMode(Constraints.Infinity))
    }
}
