package presentation.history

sealed interface ProcessingHistoryIntent {
    data object Load : ProcessingHistoryIntent
    data class ChangeFilter(val type: String?) : ProcessingHistoryIntent
    data class DeleteItem(val id: String) : ProcessingHistoryIntent
    data object ClearAll : ProcessingHistoryIntent
    data object NavigateBack : ProcessingHistoryIntent
}
