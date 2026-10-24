package com.setiker.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import presentation.components.NeubrutalSnackbarContent
import presentation.theme.SetikerTheme

/**
 * The screenshot of the error snackbar.
 *
 * The error is the one message in the app that has to look like a failure. If it drifts into
 * looking like a note, users stop reading it, and an error nobody reads is an error nobody
 * fixes. The reference image is the contract.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = ScreenshotTestApplication::class
)
class ErrorSnackbarScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun theErrorSnackbar() {
        composeTestRule.setContent {
            SetikerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    NeubrutalSnackbarContent(
                        message = "Can't reach the server. Try again in a moment.",
                        isError = true,
                        // The test renderer runs the animation to its end, so the timer is held
                        // still here: the square is drawn at two thirds.
                        timer = Animatable(0.65f)
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage("error_snackbar.png")
    }

    @Test
    fun theErrorSnackbarWithALongMessage() {
        composeTestRule.setContent {
            SetikerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    NeubrutalSnackbarContent(
                        message = "The pack could not be saved because the cloud refused the " +
                            "upload and the local copy is out of date. Try again in a moment.",
                        isError = true,
                        // Held still for the same reason as the test above: the renderer runs
                        // the countdown to its end, and the end is not the state to review.
                        timer = Animatable(0.65f)
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage("error_snackbar_long.png")
    }
}
