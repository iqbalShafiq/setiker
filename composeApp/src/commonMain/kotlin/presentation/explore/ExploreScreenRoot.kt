package presentation.explore

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun ExploreScreenRoot(
    onBackClick: () -> Unit,
    onPackClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: ExploreViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ExploreEffect.NavigateBack -> onBackClick()
                ExploreEffect.NavigateHistory -> onHistoryClick()
                is ExploreEffect.NavigateToPublicPack -> onPackClick(effect.packId)
                is ExploreEffect.ShowError -> snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
            }
        }
    }

    ExploreScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
