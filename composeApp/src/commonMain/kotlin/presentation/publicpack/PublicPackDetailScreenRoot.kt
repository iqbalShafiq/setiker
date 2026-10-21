package presentation.publicpack

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
fun PublicPackDetailScreenRoot(
    packId: String,
    onBackClick: () -> Unit,
    onNavigateToLocalPack: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToCreator: (String) -> Unit,
    viewModel: PublicPackDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }

    LaunchedEffect(packId) {
        viewModel.onIntent(PublicPackDetailIntent.Load(packId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PublicPackDetailEffect.NavigateBack -> onBackClick()
                PublicPackDetailEffect.NavigateToLogin -> onNavigateToLogin()
                is PublicPackDetailEffect.NavigateToLocalPack -> onNavigateToLocalPack(effect.localPackId)
                is PublicPackDetailEffect.NavigateToCreator -> onNavigateToCreator(effect.userId)
                is PublicPackDetailEffect.ShowMessage -> snackbarHostState.showMessage(effect.message.resolveOrDefault())
            }
        }
    }

    PublicPackDetailScreen(
        packId = packId,
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
