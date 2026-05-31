package presentation.aijob

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiResultSheetVisibilityTest {
    @Test
    fun hidesSheetWhileJobInProgressEvenWithPreview() {
        assertFalse(
            AiResultSheetVisibility.shouldShowGeneratedResultsSheet(
                hasPreview = true,
                sheetVisible = true,
                isAiJobInProgress = true
            )
        )
    }

    @Test
    fun showsSheetWhenPreviewVisibleAndIdle() {
        assertTrue(
            AiResultSheetVisibility.shouldShowGeneratedResultsSheet(
                hasPreview = true,
                sheetVisible = true,
                isAiJobInProgress = false
            )
        )
    }

    @Test
    fun hidesSheetWhenDismissedButPreviewRetained() {
        assertFalse(
            AiResultSheetVisibility.shouldShowGeneratedResultsSheet(
                hasPreview = true,
                sheetVisible = false,
                isAiJobInProgress = false
            )
        )
    }
}
