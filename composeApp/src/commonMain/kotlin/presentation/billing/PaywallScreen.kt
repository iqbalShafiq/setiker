package presentation.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.billing.BillingProductUiModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AiQuotaSummary
import presentation.components.AppIllustration
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.ContentLoadLayout
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.paywall_buy
import setiker.composeapp.generated.resources.paywall_empty_desc
import setiker.composeapp.generated.resources.paywall_empty_title
import setiker.composeapp.generated.resources.paywall_load_failed_desc
import setiker.composeapp.generated.resources.paywall_load_failed_title
import setiker.composeapp.generated.resources.paywall_processing
import setiker.composeapp.generated.resources.paywall_purchase_history
import setiker.composeapp.generated.resources.paywall_restore
import setiker.composeapp.generated.resources.paywall_title
import setiker.composeapp.generated.resources.paywall_tokens_label
import setiker.composeapp.generated.resources.retry

@Composable
fun PaywallScreenRoot(
    onBack: () -> Unit,
    onNavigateToPurchaseHistory: () -> Unit = {},
    viewModel: PaywallViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.resolveOrDefault())
            viewModel.onIntent(PaywallIntent.DismissMessage)
        }
        state.error?.let {
            if (!state.loadFailed || state.products.isNotEmpty()) {
                snackbarHostState.showSnackbar(it.resolveOrDefault())
                viewModel.onIntent(PaywallIntent.DismissMessage)
            }
        }
    }

    PaywallScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onNavigateToPurchaseHistory = onNavigateToPurchaseHistory
    )
}

@Composable
fun PaywallScreen(
    state: PaywallState,
    snackbarHostState: SnackbarHostState,
    onIntent: (PaywallIntent) -> Unit,
    onBack: () -> Unit,
    onNavigateToPurchaseHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.paywall_title),
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        ContentLoadLayout(
            isLoading = state.isLoading,
            loadFailed = state.loadFailed,
            isEmpty = state.products.isEmpty(),
            emptyTitle = stringResource(Res.string.paywall_empty_title),
            emptyDescription = stringResource(Res.string.paywall_empty_desc),
            errorTitle = stringResource(Res.string.paywall_load_failed_title),
            onRetry = { onIntent(PaywallIntent.Load) },
            emptyIllustration = AppIllustration.SearchEmpty,
            errorIllustration = AppIllustration.ErrorState,
            hasContent = state.products.isNotEmpty(),
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item("quota") {
                    AiQuotaSummary(
                        usage = state.aiUsage,
                        isLoading = state.isLoadingAiUsage,
                        hasError = state.aiUsageLoadFailed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(state.products, key = { it.code }) { product ->
                    PaywallProductCard(
                        product = product,
                        purchasingCode = state.purchasingCode,
                        onPurchase = { onIntent(PaywallIntent.Purchase(product.code)) }
                    )
                }
                item("actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppSecondaryButton(
                            text = stringResource(Res.string.paywall_restore),
                            onClick = { onIntent(PaywallIntent.Restore) },
                            enabled = state.purchasingCode == null && !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AppSecondaryButton(
                            text = stringResource(Res.string.paywall_purchase_history),
                            onClick = onNavigateToPurchaseHistory,
                            enabled = state.purchasingCode == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallProductCard(
    product: BillingProductUiModel,
    purchasingCode: String?,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = neubrutalShadowColor()
            )
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(neubrutalCardSurface())
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface()
        )
        product.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalMutedOnSurface()
            )
        }
        product.formattedPrice?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = neubrutalOnSurface()
            )
        }
        product.tokenAmount?.let {
            Text(
                text = stringResource(Res.string.paywall_tokens_label, it),
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
        }
        AppPrimaryButton(
            text = if (purchasingCode == product.code) {
                stringResource(Res.string.paywall_processing)
            } else {
                stringResource(Res.string.paywall_buy)
            },
            onClick = onPurchase,
            enabled = purchasingCode == null,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
