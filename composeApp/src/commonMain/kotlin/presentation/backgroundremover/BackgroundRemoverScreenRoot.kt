package presentation.backgroundremover

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import presentation.common.resolveOrDefault

@Composable
fun BackgroundRemoverScreenRoot(
    imagePath: String,
    onBackClick: () -> Unit,
    onBackgroundRemoved: (String) -> Unit,
    viewModel: BackgroundRemoverViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(imagePath) {
        viewModel.onIntent(BackgroundRemoverIntent.LoadImage(imagePath))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BackgroundRemoverEffect.BackgroundRemoved -> onBackgroundRemoved(effect.path)
                is BackgroundRemoverEffect.NavigateBack -> onBackClick()
                is BackgroundRemoverEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
            }
        }
    }

    BackgroundRemoverScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
