package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import presentation.components.AppPrimaryButton
import presentation.components.EmptyState
import presentation.components.NeubrutalIconButton
import presentation.components.PageIndicator
import presentation.theme.SetikerTheme

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
}
