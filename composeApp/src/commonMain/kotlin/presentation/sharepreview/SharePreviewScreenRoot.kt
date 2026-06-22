package presentation.sharepreview

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun SharePreviewScreenRoot(
    kind: String,
    token: String,
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToLocalPack: (String) -> Unit,
    viewModel: SharePreviewViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(kind, token) {
        viewModel.onIntent(SharePreviewIntent.Load(kind, token))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SharePreviewEffect.NavigateBack -> onBackClick()
                SharePreviewEffect.NavigateToLogin -> onNavigateToLogin()
                is SharePreviewEffect.NavigateToLocalPack -> onNavigateToLocalPack(effect.packId)
                is SharePreviewEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
            }
        }
    }

    SharePreviewScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
