package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import presentation.components.AppTextField
import presentation.components.NeubrutalCheckbox
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.NeubrutalAddTagPill
import presentation.components.PackBottomBarIconButton
import presentation.components.NeubrutalAddTagPill
import presentation.icons.SetikerIcons
import presentation.theme.SetikerTheme

/**
 * Disabled controls, side by side with their enabled twins.
 *
 * Nothing covered this state before: the screenshot tests only ever rendered controls that were on.
 * That is how the text field ended up keeping a full-strength outline while the button next to it
 * faded — two answers to the same question, and no image that showed the difference.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = ScreenshotTestApplication::class
)
class DisabledStateScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyDisabledControlIsFainterThanItsEnabledTwin() {
        composeTestRule.setContent {
            SetikerTheme {
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFFFDF5))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Field(enabled = true)
                        Field(enabled = false)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(enabled = true)
                        Box(enabled = false)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Fab(enabled = true)
                        Fab(enabled = false)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        BarButton(enabled = true)
                        BarButton(enabled = false)
                        NeubrutalAddTagPill(label = "Add", onClick = {}, enabled = true)
                        NeubrutalAddTagPill(label = "Add", onClick = {}, enabled = false)
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("disabled_states.png")
    }

    @Composable
    private fun Field(enabled: Boolean) {
        AppTextField(
            value = if (enabled) "Enabled" else "Disabled",
            onValueChange = {},
            label = "Pack name",
            enabled = enabled,
            modifier = Modifier.width(120.dp)
        )
    }

    @Composable
    private fun Box(enabled: Boolean) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeubrutalCheckbox(checked = true, enabled = enabled)
            NeubrutalCheckbox(checked = false, enabled = enabled)
        }
    }

    @Composable
    private fun BarButton(enabled: Boolean) {
        PackBottomBarIconButton(
            icon = SetikerIcons.Search,
            contentDescription = if (enabled) "Search" else "Search, unavailable",
            onClick = {},
            enabled = enabled
        )
    }

    @Composable
    private fun Fab(enabled: Boolean) {
        PackBottomBarFab(
            icon = SetikerIcons.Sparkle,
            contentDescription = if (enabled) "Generate" else "Generate, unavailable",
            onClick = {},
            enabled = enabled
        )
    }
}