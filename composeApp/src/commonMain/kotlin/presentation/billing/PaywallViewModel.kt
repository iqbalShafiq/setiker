package presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.billing.BillingProductUiModel
import domain.billing.BillingRepository
import domain.billing.EntitlementSnapshot
import domain.model.AiUsage
import domain.repository.AiQuotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.paywall_purchase_success
import setiker.composeapp.generated.resources.paywall_restore_success

data class PaywallState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val products: List<BillingProductUiModel> = emptyList(),
    val purchasingCode: String? = null,
    val message: UiText? = null,
    val error: UiText? = null,
    val entitlement: EntitlementSnapshot? = null,
    val aiUsage: AiUsage? = null,
    val isLoadingAiUsage: Boolean = false,
    val aiUsageLoadFailed: Boolean = false
)

sealed interface PaywallIntent {
    data object Load : PaywallIntent
    data class Purchase(val productCode: String) : PaywallIntent
    data object Restore : PaywallIntent
    data object DismissMessage : PaywallIntent
    data object NavigateBack : PaywallIntent
    data object OpenPurchaseHistory : PaywallIntent
}

class PaywallViewModel(
    private val billingRepository: BillingRepository,
    private val aiQuotaRepository: AiQuotaRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    init {
        onIntent(PaywallIntent.Load)
    }

    fun onIntent(intent: PaywallIntent) {
        when (intent) {
            PaywallIntent.Load -> {
                loadProducts()
                loadEntitlement()
            }
            is PaywallIntent.Purchase -> purchase(intent.productCode)
            PaywallIntent.Restore -> restore()
            PaywallIntent.DismissMessage -> _state.update { it.copy(message = null, error = null) }
            PaywallIntent.NavigateBack, PaywallIntent.OpenPurchaseHistory -> Unit
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false, error = null) }
            runCatching { billingRepository.getProducts() }
                .onSuccess { products ->
                    _state.update {
                        it.copy(isLoading = false, loadFailed = false, products = products)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = true,
                            error = UiText.DynamicString(error.message ?: "Failed to load products")
                        )
                    }
                }
        }
    }

    private fun loadEntitlement() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAiUsage = true, aiUsageLoadFailed = false) }
            val entitlement = runCatching { billingRepository.getCurrentEntitlement() }.getOrNull()
            val usage = aiQuotaRepository.getUsage(forceRefresh = true)
            _state.update {
                it.copy(
                    entitlement = entitlement,
                    aiUsage = usage,
                    isLoadingAiUsage = false,
                    aiUsageLoadFailed = usage == null
                )
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
                    message = if (result.success) {
                        UiText.StringRes(Res.string.paywall_purchase_success)
                    } else {
                        null
                    },
                    error = result.errorMessage?.let(UiText::DynamicString)
                )
            }
            if (result.success) {
                loadEntitlement()
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
                    message = UiText.StringRes(
                        Res.string.paywall_restore_success,
                        listOf(result.restoredCount)
                    ),
                    error = result.errorMessage?.let(UiText::DynamicString)
                )
            }
            loadEntitlement()
        }
    }
}
