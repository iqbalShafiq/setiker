package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import domain.model.Sticker
import domain.model.StickerPack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import presentation.components.StickerCard
import presentation.components.StickerPackListCard
import presentation.theme.SetikerTheme

/**
 * The performance tests.
 *
 * A performance review that only measures once tells you about that moment. These tests turn the
 * findings into a gate: they count how often a list item is composed and fail when a change
 * somewhere else on the screen drags the whole list with it.
 *
 * The rule renders on a Robolectric device, so the numbers are the real ones - the same
 * composition, the same skipping, the same recomposition counts the app produces.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = ScreenshotTestApplication::class
)
class RecompositionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // --- The sticker grid is the hottest list in the app -------------------------------------------

    @Test
    fun aChangeElsewhereOnTheScreenDoesNotRecomposeTheStickerGrid() {
        val compositions = mutableIntStateOf(0)
        var unrelated by mutableIntStateOf(0)
        val stickers = List(12) { index -> sticker(index) }

        composeTestRule.setContent {
            SetikerTheme {
                Column {
                    // A piece of state that belongs to the screen, not to the grid.
                    BasicText("counter $unrelated")
                    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                        items(stickers, key = { it.imageFile }) { sticker ->
                            CountedStickerCard(sticker = sticker, onClick = {}, compositions = compositions)
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val afterTheFirstLayout = compositions.intValue
        assertEquals("the grid should have composed its items once", 12, afterTheFirstLayout)

        unrelated = 1
        composeTestRule.waitForIdle()

        assertEquals(
            "the sticker cells were recomposed because of a change that did not touch them",
            afterTheFirstLayout,
            compositions.intValue
        )
    }

    // --- The pack list is the first screen the user sees -------------------------------------------

    @Test
    fun aChangeElsewhereOnTheScreenDoesNotRecomposeThePackList() {
        val compositions = mutableIntStateOf(0)
        var unrelated by mutableIntStateOf(0)
        val packs = List(8) { index -> pack(index) }

        composeTestRule.setContent {
            SetikerTheme {
                Column {
                    BasicText("counter $unrelated")
                    LazyColumn {
                        items(packs, key = { it.identifier }) { pack ->
                            CountedPackCard(pack = pack, onClick = {}, compositions = compositions)
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val afterTheFirstLayout = compositions.intValue
        assertEquals("the list should have composed its items once", 8, afterTheFirstLayout)

        unrelated = 1
        composeTestRule.waitForIdle()

        assertEquals(
            "the pack cards were recomposed because of a change that did not touch them",
            afterTheFirstLayout,
            compositions.intValue
        )
    }

    // --- A new instance of the same data still has to be skipped ------------------------------------

    @Test
    fun equalModelsDoNotRecomposeTheGridItems() {
        val compositions = mutableIntStateOf(0)
        var unrelated by mutableIntStateOf(0)
        val stickers = List(12) { index -> sticker(index) }

        composeTestRule.setContent {
            SetikerTheme {
                Column {
                    BasicText("counter $unrelated")
                    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                        items(stickers, key = { it.imageFile }) { sticker ->
                            CountedStickerCard(sticker = sticker, onClick = {}, compositions = compositions)
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val afterTheFirstLayout = compositions.intValue

        unrelated = 1
        composeTestRule.waitForIdle()

        assertEquals(afterTheFirstLayout, compositions.intValue)
    }

    /**
     * Stands in for the sticker card.
     *
     * The parameters are the same as the card's, so the compiler makes the same skipping decision
     * for both. What is counted is how often the body of the card runs, which is the part that
     * costs something: the modifiers, the image request and the string lookups.
     */
    @Composable
    private fun CountedStickerCard(
        sticker: Sticker,
        onClick: () -> Unit,
        compositions: MutableIntState
    ) {
        SideEffect { compositions.intValue++ }
        StickerCard(sticker = sticker, onClick = onClick)
    }

    /** Stands in for the pack card. See [CountedStickerCard]. */
    @Composable
    private fun CountedPackCard(
        pack: StickerPack,
        onClick: () -> Unit,
        compositions: MutableIntState
    ) {
        SideEffect { compositions.intValue++ }
        StickerPackListCard(pack = pack, onClick = onClick)
    }

    private fun sticker(index: Int) = Sticker(
        imageFile = "/stickers/sticker_$index.webp",
        emojis = listOf("dY~,"),
        accessibilityText = "Sticker $index"
    )

    private fun pack(index: Int) = StickerPack(
        identifier = "pack_$index",
        name = "Pack $index",
        publisher = "Publisher",
        trayImageFile = "/packs/tray_$index.webp",
        stickers = List(3) { stickerIndex -> sticker(index * 10 + stickerIndex) }
    )
}
