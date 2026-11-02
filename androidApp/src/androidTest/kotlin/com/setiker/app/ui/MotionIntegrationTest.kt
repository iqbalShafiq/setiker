package com.setiker.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import presentation.components.AppPrimaryButton
import presentation.components.AppTextField
import presentation.components.BottomSheetOptionRow
import presentation.components.ContentLoadLayout
import presentation.components.LoadingDots
import presentation.components.NeubrutalProgress
import presentation.components.NeubrutalSelectableChip
import presentation.components.SkeletonPackList
import presentation.theme.SetikerTheme

/**
 * Integration tests for the motion and loading surfaces.
 *
 * They run on a device and exercise the real Compose tree: the animated content helper, the
 * selection semantics, the progress semantics, and the components that replaced the Material
 * spinners. The tests are deliberately about behaviour after the motion settles; the feel of the
 * animation itself is verified on the device with the Android CLI and the screenshot suite.
 */
@OptIn(ExperimentalTestApi::class)
class MotionIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingDotsExposeAStatusDescription() {
        composeTestRule.setContent {
            SetikerTheme {
                LoadingDots(contentDescription = "Loading packs")
            }
        }

        composeTestRule.onNodeWithContentDescription("Loading packs").assertExists()
    }

    @Test
    fun progressBarShowsTheAnimatedPercentage() {
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalProgress(
                    progress = 0.42f,
                    label = "Encoding"
                )
            }
        }

        composeTestRule.onNodeWithText("42%").assertExists()
        composeTestRule.onNodeWithText("Encoding").assertExists()
    }

    @Test
    fun selectableChipReportsItsSelection() {
        var clicks = 0
        composeTestRule.setContent {
            SetikerTheme {
                NeubrutalSelectableChip(
                    label = "Cats",
                    selected = true,
                    onClick = { clicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Cats").assertIsSelected()
        composeTestRule.onNodeWithText("Cats").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun sheetOptionRowForwardsItsClick() {
        var clicks = 0
        composeTestRule.setContent {
            SetikerTheme {
                BottomSheetOptionRow(
                    label = "Newest",
                    selected = false,
                    onClick = { clicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Newest").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun contentLoadLayoutEndsOnTheContentPhase() {
        var loading by mutableStateOf(true)
        composeTestRule.setContent {
            SetikerTheme {
                ContentLoadLayout(
                    isLoading = loading,
                    loadFailed = false,
                    isEmpty = false,
                    emptyTitle = "Nothing here",
                    emptyDescription = "",
                    errorTitle = "Could not load",
                    onRetry = {},
                    hasContent = !loading
                ) {
                    AppPrimaryButton(text = "Pack list", onClick = {})
                }
            }
        }

        assertEquals(
            0,
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasText("Pack list")
            ).fetchSemanticsNodes().size
        )

        composeTestRule.runOnUiThread { loading = false }
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasText("Pack list")
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Pack list").assertExists()
    }

    @Test
    fun imeNextMovesFocusToTheNextField() {
        var first by mutableStateOf("")
        var second by mutableStateOf("")
        composeTestRule.setContent {
            SetikerTheme {
                androidx.compose.foundation.layout.Column {
                    AppTextField(
                        value = first,
                        onValueChange = { first = it },
                        label = "First",
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    )
                    AppTextField(
                        value = second,
                        onValueChange = { second = it },
                        label = "Second",
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    )
                }
            }
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performImeAction()
        composeTestRule.onAllNodes(hasSetTextAction())[1].assertIsFocused()
    }

    @Test
    fun imeNextMovesFocusToANonTextFieldControl() {
        var value by mutableStateOf("")
        var checked by mutableStateOf(false)
        composeTestRule.setContent {
            SetikerTheme {
                Column {
                    AppTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = "Reason",
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    )
                    Row(
                        modifier = Modifier
                            .testTag("confirmation")
                            .focusable()
                            .toggleable(
                                value = checked,
                                onValueChange = { checked = it },
                                role = Role.Checkbox
                            )
                    ) {
                        BasicText("I understand")
                    }
                }
            }
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performImeAction()
        composeTestRule.onNodeWithTag("confirmation").assertIsFocused()
    }

    @Test
    fun imeDoneRunsTheFormAction() {
        var value by mutableStateOf("")
        var submitted = 0
        composeTestRule.setContent {
            SetikerTheme {
                AppTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Only field",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    onDone = { submitted++ }
                )
            }
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performImeAction()
        assertEquals(1, submitted)
    }

    @Test
    fun skeletonListPassesTheAccessibilityChecks() {
        composeTestRule.setContent {
            SetikerTheme {
                SkeletonPackList(rows = 2)
            }
        }

        composeTestRule.enableAccessibilityChecks()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
