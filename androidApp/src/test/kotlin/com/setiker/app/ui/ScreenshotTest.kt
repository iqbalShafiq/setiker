package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.Alignment
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
import presentation.components.AppPrimaryButton
import presentation.components.CelebrationBadge
import presentation.components.EmptyState
import presentation.components.NeubrutalIconButton
import presentation.components.NeubrutalCheckbox
import presentation.components.NeubrutalProgress
import presentation.components.NeubrutalRangeSlider
import presentation.components.NeubrutalSelectableChip
import presentation.components.NeubrutalSlider
import presentation.components.PageIndicator
import presentation.theme.SetikerTheme
import presentation.theme.neubrutalScreenBackground

/**
 * The screenshot tests.
 *
 * They render the components on a Robolectric device with native graphics, so the reference images
 * come out of the same drawing code the app runs - no device and no emulator needed.
 *
 * Record the references with:
 *
 * ```
 * ./gradlew :androidApp:recordRoborazziDebug
 * ```
 *
 * and verify them with:
 *
 * ```
 * ./gradlew :androidApp:verifyRoborazziDebug
 * ```
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = ScreenshotTestApplication::class
)
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState() {
        composeTestRule.setContent {
            SetikerTheme {
                EmptyState(
                    title = "No packs found",
                    description = "Try a different search term.",
                    action = { AppPrimaryButton(text = "Create your first pack", onClick = {}) }
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("empty_state.png")
    }

    @Test
    fun disabledButton() {
        composeTestRule.setContent {
            SetikerTheme {
                AppPrimaryButton(text = "Add decoration", onClick = {}, enabled = false)
            }
        }

        composeTestRule.onRoot().captureRoboImage("disabled_button.png")
    }

    @Test
    fun pageIndicator() {
        composeTestRule.setContent {
            SetikerTheme {
                PageIndicator(pageCount = 3, currentPage = 1)
            }
        }

        composeTestRule.onRoot().captureRoboImage("page_indicator.png")
    }

    @Test
    fun iconButton() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalIconButton(
                    icon = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    onClick = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("icon_button.png")
    }

    @Test
    fun selectedChip() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalSelectableChip(
                    label = "Cats",
                    selected = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("selected_chip.png")
    }

    @Test
    fun progress() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalProgress(
                    progress = 0.42f,
                    label = "Encoding",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("progress.png")
    }

    @Test
    fun slider() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalSlider(
                    value = 0.42f,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("slider.png")
    }

    @Test
    fun rangeSlider() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalRangeSlider(
                    value = 0.2f..0.7f,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("range_slider.png")
    }

    @Test
    fun checkedCheckbox() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalCheckbox(checked = true)
            }
        }

        composeTestRule.onRoot().captureRoboImage("checked_checkbox.png")
    }

    @Test
    fun celebrationBadge() {
        composeTestRule.setContent {
            SetikerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(neubrutalScreenBackground())
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CelebrationBadge(
                        visible = true,
                        contentDescription = "Sticker saved"
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage("celebration_badge.png")
    }
}
