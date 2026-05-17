package presentation.history

import data.remote.model.ProcessingHistoryItem

data class ProcessingHistoryState(
    val isLoading: Boolean = true,
    val isClearing: Boolean = false,
    val error: String? = null,
    val typeFilter: String? = null,
    val items: List<ProcessingHistoryItem> = emptyList()
)
