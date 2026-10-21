package presentation.explore

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState

@Composable
fun ExploreScreenRoot(
    onBackClick: () -> Unit,
    onPackClick: (String) -> Unit,
    onCreatorClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: ExploreViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ExploreEffect.NavigateBack -> onBackClick()
                ExploreEffect.NavigateHistory -> onHistoryClick()
                is ExploreEffect.NavigateToPublicPack -> onPackClick(effect.packId)
                is ExploreEffect.NavigateToCreator -> onCreatorClick(effect.userId)
                ExploreEffect.NavigateLogin -> onLoginClick()
                is ExploreEffect.ShowError -> snackbarHostState.showMessage(effect.message.resolveOrDefault())
            }
        }
    }

    ExploreScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
