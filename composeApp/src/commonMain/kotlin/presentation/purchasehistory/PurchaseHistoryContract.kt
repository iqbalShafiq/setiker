package presentation.purchasehistory

import domain.billing.PurchaseHistoryItem
import presentation.common.UiText

data class PurchaseHistoryState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val items: List<PurchaseHistoryItem> = emptyList(),
    val offset: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false
)

sealed interface PurchaseHistoryIntent {
    data object Load : PurchaseHistoryIntent
    data object LoadMore : PurchaseHistoryIntent
    data object Retry : PurchaseHistoryIntent
    data object NavigateBack : PurchaseHistoryIntent
}

sealed interface PurchaseHistoryEffect {
    data object NavigateBack : PurchaseHistoryEffect
    data class ShowMessage(val message: UiText) : PurchaseHistoryEffect
}
