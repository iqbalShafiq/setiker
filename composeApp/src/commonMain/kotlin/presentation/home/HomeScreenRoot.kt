package presentation.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

@Composable
fun HomeScreenRoot(
    onPackClick: (String) -> Unit,
    onCreatePackClick: () -> Unit,
    viewModel: HomeViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToPackDetail -> onPackClick(effect.packId)
                is HomeEffect.NavigateToCreatePack -> onCreatePackClick()
                is HomeEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is HomeEffect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onPackClick = onPackClick,
        snackbarHostState = snackbarHostState
    )
}
