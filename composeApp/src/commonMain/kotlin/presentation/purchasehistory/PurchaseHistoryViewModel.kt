package presentation.purchasehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.billing.BillingRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseHistoryViewModel(
    private val billingRepository: BillingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PurchaseHistoryState())
    val state: StateFlow<PurchaseHistoryState> = _state.asStateFlow()

    private val _effect = Channel<PurchaseHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(PurchaseHistoryIntent.Load)
    }

    fun onIntent(intent: PurchaseHistoryIntent) {
        when (intent) {
            PurchaseHistoryIntent.Load, PurchaseHistoryIntent.Retry -> load()
            PurchaseHistoryIntent.LoadMore -> loadMore()
            PurchaseHistoryIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(PurchaseHistoryEffect.NavigateBack)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    loadFailed = false,
                    offset = 0,
                    hasMore = false,
                    isLoadingMore = false
                )
            }
            runCatching {
                billingRepository.getPurchaseHistory(limit = PAGE_SIZE, offset = 0)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = false,
                        items = page.purchases,
                        offset = page.purchases.size,
                        hasMore = page.hasMore
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(isLoading = false, loadFailed = true, items = emptyList())
                }
            }
        }
    }

    private fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoadingMore || current.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val offset = current.offset
            runCatching {
                billingRepository.getPurchaseHistory(limit = PAGE_SIZE, offset = offset)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        items = it.items + page.purchases,
                        offset = it.offset + page.purchases.size,
                        hasMore = page.hasMore
                    )
                }
            }.onFailure {
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
