package presentation.aijob

/**
 * Shared rules for when AI result bottom sheets should be composed.
 * Previews may remain in state after the user dismisses the sheet; only show the sheet
 * when explicitly visible and not while a new AI job is in progress.
 */
object AiResultSheetVisibility {
    fun shouldShowGeneratedResultsSheet(
        hasPreview: Boolean,
        sheetVisible: Boolean,
        isAiJobInProgress: Boolean
    ): Boolean = hasPreview && sheetVisible && !isAiJobInProgress
}
