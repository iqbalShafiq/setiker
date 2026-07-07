package presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.billing.BillingProductUiModel
import domain.billing.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaywallState(
    val isLoading: Boolean = true,
    val products: List<BillingProductUiModel> = emptyList(),
    val purchasingCode: String? = null,
    val message: String? = null,
    val error: String? = null
)

sealed interface PaywallIntent {
    data object Load : PaywallIntent
    data class Purchase(val productCode: String) : PaywallIntent
    data object Restore : PaywallIntent
    data object DismissMessage : PaywallIntent
}

class PaywallViewModel(
    private val billingRepository: BillingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    init {
        onIntent(PaywallIntent.Load)
    }

    fun onIntent(intent: PaywallIntent) {
        when (intent) {
            PaywallIntent.Load -> loadProducts()
            is PaywallIntent.Purchase -> purchase(intent.productCode)
            PaywallIntent.Restore -> restore()
            PaywallIntent.DismissMessage -> _state.update { it.copy(message = null, error = null) }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { billingRepository.getProducts() }
                .onSuccess { products ->
                    _state.update { it.copy(isLoading = false, products = products) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun purchase(productCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(purchasingCode = productCode, error = null) }
            val result = billingRepository.purchase(productCode)
            _state.update {
                it.copy(
                    purchasingCode = null,
                    message = if (result.success) "Purchase successful" else null,
                    error = result.errorMessage
                )
            }
        }
    }

    private fun restore() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = billingRepository.restorePurchases()
            _state.update {
                it.copy(
                    isLoading = false,
                    message = "Restored ${result.restoredCount} purchase(s)",
                    error = result.errorMessage
                )
            }
        }
    }
}
