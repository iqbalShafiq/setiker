package presentation.history

sealed interface ProcessingHistoryIntent {
    data object Load : ProcessingHistoryIntent
    data class ChangeFilter(val type: String?) : ProcessingHistoryIntent
    data class DeleteItem(val id: String) : ProcessingHistoryIntent
    data object ClearAll : ProcessingHistoryIntent
    data object NavigateBack : ProcessingHistoryIntent
    data class ShowReport(val id: String) : ProcessingHistoryIntent
    data object DismissReport : ProcessingHistoryIntent
    data class SelectReportReason(val reason: String) : ProcessingHistoryIntent
    data class UpdateReportDetails(val value: String) : ProcessingHistoryIntent
    data object SubmitReport : ProcessingHistoryIntent
}
