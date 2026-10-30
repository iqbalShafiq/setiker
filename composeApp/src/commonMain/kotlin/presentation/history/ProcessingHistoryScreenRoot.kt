package presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState

@Composable
fun ProcessingHistoryScreenRoot(
    onBackClick: () -> Unit,
    viewModel: ProcessingHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(ProcessingHistoryIntent.Load)
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProcessingHistoryEffect.NavigateBack -> onBackClick()
                is ProcessingHistoryEffect.ShowMessage -> snackbarHostState.showMessage(effect.message.resolveOrDefault())
            }
        }
    }

    ProcessingHistoryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
