package presentation.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun ProcessingHistoryScreenRoot(
    onBackClick: () -> Unit,
    viewModel: ProcessingHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(ProcessingHistoryIntent.Load)
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProcessingHistoryEffect.NavigateBack -> onBackClick()
                is ProcessingHistoryEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
            }
        }
    }

    ProcessingHistoryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
