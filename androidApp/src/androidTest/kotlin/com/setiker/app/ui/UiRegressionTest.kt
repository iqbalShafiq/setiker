package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import presentation.components.AppPrimaryButton
import presentation.components.EmptyState
import presentation.components.NeubrutalIconButton
import presentation.components.PageIndicator
import presentation.theme.SetikerTheme

/**
 * The UI tests that lock in the findings of the UI/UX and motion audit.
 *
 * They run on a device (or an emulator) through the `androidApp` module, so they exercise the
 * real Compose tree, the real semantics and the real accessibility checks.
 */
@OptIn(ExperimentalTestApi::class)
class UiRegressionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // --- The empty state kept its call to action while squeezing it out of existence -------------

    @Test
    fun emptyStateShowsTitleDescriptionAndAction() {
        composeTestRule.setContent {
            SetikerTheme {
                EmptyState(
                    title = "No packs found",
                    description = "Try a different search term.",
                    action = { AppPrimaryButton(text = "Create your first pack", onClick = {}) }
                )
            }
        }

        composeTestRule.onNodeWithText("No packs found").assertExists()
        composeTestRule.onNodeWithText("Try a different search term.").assertExists()
        composeTestRule.onNodeWithText("Create your first pack").assertExists()
    }

    @Test
    fun emptyStateActionIsReachable() {
        var clicks = 0
        composeTestRule.setContent {
            SetikerTheme {
                EmptyState(
                    title = "No packs found",
                    description = "Try a different search term.",
                    action = { AppPrimaryButton(text = "Create your first pack", onClick = { clicks++ }) }
                )
            }
        }

        composeTestRule.onNodeWithText("Create your first pack").assertIsEnabled()
        composeTestRule.onNodeWithText("Create your first pack").performClick()

        assertEquals(1, clicks)
    }

    // --- A disabled button used to look enabled ---------------------------------------------------

    @Test
    fun disabledButtonIsNotEnabled() {
        composeTestRule.setContent {
            SetikerTheme {
                AppPrimaryButton(text = "Add decoration", onClick = {}, enabled = false)
            }
        }

        composeTestRule.onNodeWithText("Add decoration").assertIsNotEnabled()
    }

    // --- The page dots used to be too faint to see -----------------------------------------------

    @Test
    fun pageIndicatorAnnouncesTheCurrentPage() {
        composeTestRule.setContent {
            SetikerTheme {
                PageIndicator(pageCount = 3, currentPage = 1)
            }
        }

        composeTestRule.onNodeWithContentDescription("Page 2 of 3").assertExists()
    }

    // --- The icon buttons had no press feedback and a 32 dp target -------------------------------

    @Test
    fun iconButtonIsLabelledAndClickable() {
        var clicks = 0
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalIconButton(
                    icon = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    onClick = { clicks++ }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        assertEquals(1, clicks)
    }

    // --- The accessibility checks ------------------------------------------------------------------

    @Test
    fun emptyStatePassesTheAccessibilityChecks() {
        composeTestRule.setContent {
            SetikerTheme {
                EmptyState(
                    title = "No packs found",
                    description = "Try a different search term.",
                    action = { AppPrimaryButton(text = "Create your first pack", onClick = {}) }
                )
            }
        }

        composeTestRule.enableAccessibilityChecks()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun iconButtonPassesTheAccessibilityChecks() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalIconButton(
                    icon = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    onClick = {}
                )
            }
        }

        composeTestRule.enableAccessibilityChecks()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
