package presentation.purchasehistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.billing.PurchaseHistoryItem
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.components.AppIllustration
import presentation.components.AppTopBar
import presentation.components.ContentLoadLayout
import presentation.theme.AccentCoral
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
import setiker.composeapp.generated.resources.purchase_history_empty_desc
import setiker.composeapp.generated.resources.purchase_history_empty_title
import setiker.composeapp.generated.resources.purchase_history_load_failed
import setiker.composeapp.generated.resources.purchase_history_title
import setiker.composeapp.generated.resources.purchase_history_tokens

@Composable
fun PurchaseHistoryScreenRoot(
    onBack: () -> Unit,
    viewModel: PurchaseHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PurchaseHistoryEffect.NavigateBack -> onBack()
                is PurchaseHistoryEffect.ShowMessage -> Unit
            }
        }
    }
    PurchaseHistoryScreen(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun PurchaseHistoryScreen(
    state: PurchaseHistoryState,
    onIntent: (PurchaseHistoryIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.purchase_history_title),
                onBackClick = { onIntent(PurchaseHistoryIntent.NavigateBack) }
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        ContentLoadLayout(
            isLoading = state.isLoading,
            loadFailed = state.loadFailed,
            isEmpty = state.items.isEmpty(),
            emptyTitle = stringResource(Res.string.purchase_history_empty_title),
            emptyDescription = stringResource(Res.string.purchase_history_empty_desc),
            errorTitle = stringResource(Res.string.purchase_history_load_failed),
            onRetry = { onIntent(PurchaseHistoryIntent.Retry) },
            emptyIllustration = AppIllustration.SearchEmpty,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    PurchaseHistoryRow(
                        item = item,
                        modifier = Modifier.animateItem()
                    )
                }
                if (state.isLoadingMore) {
                    item(key = "pagination_loader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = AccentCoral
                            )
                        }
                    }
                } else if (state.hasMore) {
                    item(key = "pagination_trigger") {
                        LaunchedEffect(state.offset) {
                            onIntent(PurchaseHistoryIntent.LoadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseHistoryRow(
    item: PurchaseHistoryItem,
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.productName ?: item.productCode,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface()
        )
        Text(
            text = "${item.status} · ${item.type} · ${item.provider}",
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalMutedOnSurface()
        )
        item.tokenAmount?.let { tokens ->
            Text(
                text = stringResource(Res.string.purchase_history_tokens, tokens),
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalOnSurface()
            )
        }
        Text(
            text = item.createdAt,
            style = MaterialTheme.typography.labelSmall,
            color = neubrutalMutedOnSurface()
        )
    }
}
