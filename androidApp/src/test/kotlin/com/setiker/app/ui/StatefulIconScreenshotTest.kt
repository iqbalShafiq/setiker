package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import presentation.components.MorphGlyphVisual
import presentation.components.MorphIconVisual
import presentation.components.StatefulIconToggle
import presentation.icons.GlyphMorph
import presentation.icons.MorphIcon
import presentation.icons.SetikerIcons
import presentation.icons.SetikerMorphs
import presentation.theme.AppIconSize
import presentation.theme.NeubrutalBg
import presentation.theme.SetikerTheme

/**
 * The stateful glyphs, at the frames that have to be reviewable.
 *
 * The fill is rendered from the same drawing code the app runs — a mask reveal over the solid glyph
 * with the stroked outline kept on top — so a drift in the border weight or the ink colour shows up
 * here rather than on a device.
 *
 * The second test matters more than the first: the animation must never be the only thing saying
 * "liked". The toggle has to expose its state to a screen reader whether or not anything moved.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = ScreenshotTestApplication::class
)
class StatefulIconScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyStatefulGlyphAtEachStageOfTheFill() {
        composeTestRule.setContent {
            SetikerTheme {
                Column(
                    modifier = Modifier
                        .background(NeubrutalBg)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    StageRow(icon = SetikerIcons.HeartMorph)
                    StageRow(icon = SetikerIcons.BookmarkMorph)
                    MorphRow(morph = SetikerMorphs.Eye, celebration = true)
                    MorphRow(morph = SetikerMorphs.PlayPause)
                    MorphRow(morph = SetikerMorphs.Loaded)
                    MorphRow(morph = SetikerMorphs.SearchClose)
                    MorphRow(morph = SetikerMorphs.Follow)
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage("stateful_icons.png")
    }

    @Test
    fun theToggleExposesItsStateWithoutAnyMotion() {
        composeTestRule.setContent {
            SetikerTheme {
                StatefulIconToggle(
                    icon = SetikerIcons.HeartMorph,
                    checked = true,
                    onCheckedChange = {},
                    contentDescription = "Like",
                    size = 48.dp
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Like")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            // The payoff of being a real toggle rather than a clickable with a hand-written
            // description: the state is queryable, so a regression cannot hide behind wording.
            .assertIsOn()
    }

    @Test
    fun theToggleReportsOffWhenItIsOff() {
        composeTestRule.setContent {
            SetikerTheme {
                StatefulIconToggle(
                    icon = SetikerIcons.BookmarkMorph,
                    checked = false,
                    onCheckedChange = {},
                    contentDescription = "Save",
                    size = 48.dp
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Save").assertIsOff()
    }
}

@Composable
private fun MorphRow(morph: GlyphMorph, celebration: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frame ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                MorphGlyphVisual(
                    morph = morph,
                    progress = remember { mutableStateOf(frame) },
                    size = 44.dp,
                    // The last cell holds the glyph still and runs the burst through its decay, so
                    // the celebration is reviewable the way the morph is.
                    burst = if (celebration && frame == 1f) remember { mutableStateOf(0.35f) } else null,
                    burstSpread = if (celebration) 1.25f else 1f
                )
            }
        }
    }
}

@Composable
private fun StageRow(icon: MorphIcon) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Frame(icon, progress = 0f, size = AppIconSize.Bar)
        Frame(icon, progress = 0f, size = 44.dp)
        Frame(icon, progress = 0.35f, size = 44.dp, burst = 0.3f)
        Frame(icon, progress = 0.62f, size = 44.dp)
        Frame(icon, progress = 1f, size = 44.dp)
    }
}

/**
 * A held-still progress: the renderer would otherwise run the fill to its end, and the end is
 * exactly the frame that needs no review.
 */
@Composable
private fun Frame(icon: MorphIcon, progress: Float, size: Dp, burst: Float? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        MorphIconVisual(
            icon = icon,
            progress = remember { mutableStateOf(progress) },
            size = size,
            burst = burst?.let { remember { mutableStateOf(it) } }
        )
    }
}
