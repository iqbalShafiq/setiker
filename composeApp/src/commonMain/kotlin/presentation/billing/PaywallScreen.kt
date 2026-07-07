package presentation.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreenRoot(
    onBack: () -> Unit,
    viewModel: PaywallViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(PaywallIntent.DismissMessage)
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(PaywallIntent.DismissMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Get more AI tokens") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading && state.products.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.products, key = { it.code }) { product ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    product.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    product.formattedPrice?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
                    product.tokenAmount?.let {
                        Text("$it tokens (non-expiring)", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { viewModel.onIntent(PaywallIntent.Purchase(product.code)) },
                        enabled = state.purchasingCode == null,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            if (state.purchasingCode == product.code) "Processing..." else "Buy"
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.onIntent(PaywallIntent.Restore) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Restore purchases")
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Back")
                }
            }
        }
    }
}
