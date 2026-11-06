package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import presentation.components.MorphGlyphIcon
import presentation.components.neubrutalFocusRing
import presentation.icons.SetikerMorphs
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.SetikerTheme
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalFill
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/**
 * The focus ring is the one state a normal screenshot never shows: it only appears for keyboard,
 * D-pad and switch-access users. Rendering it from an explicit flag keeps the drawing reviewable
 * even though the focus system itself cannot be driven from a JVM test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = ScreenshotTestApplication::class
)
class FocusRingScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theRingSitsInsideTheBorderWithoutTouchingIt() {
        composeTestRule.setContent {
            SetikerTheme {
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFFE5DC))
                        .padding(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(focused = false)
                    Surface(focused = true)
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("focus_ring.png")
    }

    @Composable
    private fun Surface(focused: Boolean) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .neubrutalShadow(
                    offsetX = NeubrutalSmallShadowOffset,
                    offsetY = NeubrutalSmallShadowOffset,
                    cornerRadius = NeubrutalSmallRadius,
                    color = neubrutalShadowColor()
                )
                .clip(RoundedCornerShape(NeubrutalSmallRadius))
                .neubrutalFill(color = NeubrutalWhite, cornerRadius = NeubrutalSmallRadius)
                .neubrutalBorderWithGloss(
                    color = NeubrutalBlack,
                    cornerRadius = NeubrutalSmallRadius,
                    width = NeubrutalBorderWidth,
                    shape = RoundedCornerShape(NeubrutalSmallRadius),
                    highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = false)
                )
                .neubrutalFocusRing(focused = focused, cornerRadius = NeubrutalSmallRadius),
            contentAlignment = Alignment.Center
        ) {
            MorphGlyphIcon(
                morph = SetikerMorphs.Eye,
                checked = false,
                size = 22.dp
            )
        }
    }
}