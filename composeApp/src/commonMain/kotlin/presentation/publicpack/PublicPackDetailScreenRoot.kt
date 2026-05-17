package presentation.publicpack

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun PublicPackDetailScreenRoot(
    packId: String,
    onBackClick: () -> Unit,
    onNavigateToLocalPack: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: PublicPackDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(packId) {
        viewModel.onIntent(PublicPackDetailIntent.Load(packId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PublicPackDetailEffect.NavigateBack -> onBackClick()
                PublicPackDetailEffect.NavigateToLogin -> onNavigateToLogin()
                is PublicPackDetailEffect.NavigateToLocalPack -> onNavigateToLocalPack(effect.localPackId)
                is PublicPackDetailEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
            }
        }
    }

    PublicPackDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
