package presentation.history

import data.remote.model.ProcessingHistoryItem

data class ProcessingHistoryState(
    val isLoading: Boolean = true,
    val isClearing: Boolean = false,
    val error: String? = null,
    val isShowingCachedData: Boolean = false,
    val typeFilter: String? = null,
    val items: List<ProcessingHistoryItem> = emptyList(),
    val showReportSheet: Boolean = false,
    val reportTargetId: String? = null,
    val reportReason: String? = null,
    val reportDetails: String = "",
    val isSubmittingReport: Boolean = false
)
