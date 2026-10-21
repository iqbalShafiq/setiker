package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import presentation.components.AppTextField
import presentation.components.NeubrutalSnackbarContent
import presentation.theme.SetikerTheme

/**
 * The error states have to reach the people who cannot see them.
 *
 * A red border is invisible to a screen reader, and a message that appears without being
 * announced is a message a screen reader user never learns about. These tests hold both
 * properties in place.
 */
class ErrorSemanticsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theErrorSnackbarIsAnnouncedWhenItAppears() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalSnackbarContent(
                    message = "Can't reach the server. Try again in a moment.",
                    isError = true
                )
            }
        }

        composeTestRule.onNodeWithText("Can't reach the server. Try again in a moment.")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite
                )
            )
    }

    @Test
    fun anInvalidFieldSaysWhatIsWrongWithIt() {
        composeTestRule.setContent {
            SetikerTheme {
                AppTextField(
                    value = "not-an-email",
                    onValueChange = {},
                    label = "Email",
                    isError = true,
                    errorText = "Please enter a valid email address."
                )
            }
        }

        composeTestRule.onNode(hasSetTextAction())
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Please enter a valid email address."
                )
            )
    }
}
