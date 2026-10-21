package presentation.home

import domain.model.StickerPack
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * F2: searching for something that matches nothing must not be treated as "the user has no packs".
 *
 * `hasListContent` drives the branch between the first-run empty state ("No Stickers Yet") and the
 * list body. When it is derived from the *filtered* packs, a no-hit search falls into the first-run
 * empty state and the dedicated "No packs found" state becomes unreachable.
 */
class HomeStateSearchTest {

    @Test
    fun `hasListContent stays true when the search query filters every pack out`() {
        val state = HomeState(
            packs = listOf(pack("Cats"), pack("Dogs")),
            searchQuery = "zzzqqq"
        )

        assertTrue(state.filteredPacks.isEmpty(), "query must not match")
        assertTrue(
            state.hasListContent,
            "a no-hit search must not be reported as 'the user has no content'"
        )
    }

    @Test
    fun `hasListContent is false when the user genuinely has no packs`() {
        val state = HomeState(packs = emptyList())

        assertFalse(state.hasListContent)
    }

    @Test
    fun `hasListContent is false when the user has no packs and types a query`() {
        val state = HomeState(packs = emptyList(), searchQuery = "cats")

        assertFalse(state.hasListContent)
    }

    @Test
    fun `hasListContent is true when the search query matches at least one pack`() {
        val state = HomeState(
            packs = listOf(pack("Cats"), pack("Dogs")),
            searchQuery = "cats"
        )

        assertTrue(state.hasListContent)
        assertTrue(state.filteredPacks.size == 1)
    }

    private fun pack(name: String) = StickerPack(
        identifier = "id-$name",
        name = name,
        publisher = "publisher",
        trayImageFile = "",
        stickers = emptyList()
    )
}
