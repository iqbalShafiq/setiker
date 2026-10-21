package com.setiker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import presentation.components.NeubrutalAddTagPill
import presentation.components.StickerEmojiTagChip
import presentation.theme.SetikerTheme

/**
 * The screenshot of the tag chips.
 *
 * The chip and the add pill sit next to each other in a row, so they have to be the same height.
 * The remove button inside the chip used to be a full 48 dp touch target, which made the chip
 * twice as tall as the pill next to it. The reference image is what keeps that from coming back.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = ScreenshotTestApplication::class
)
class TagChipScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun theTagChips() {
        composeTestRule.setContent {
            SetikerTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StickerEmojiTagChip(emoji = "⭐️", onRemove = {})
                        StickerEmojiTagChip(emoji = "dY~Z", onRemove = {})
                        NeubrutalAddTagPill(label = "+ Add", onClick = {})
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage("tag_chips.png")
    }
}
