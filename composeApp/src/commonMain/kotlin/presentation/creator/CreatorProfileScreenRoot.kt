package presentation.creator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState

@Composable
fun CreatorProfileScreenRoot(
    userId: String,
    onBackClick: () -> Unit,
    onPackClick: (String) -> Unit,
    viewModel: CreatorProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.onIntent(CreatorProfileIntent.Load(userId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CreatorProfileEffect.NavigateBack -> onBackClick()
                is CreatorProfileEffect.NavigateToPack -> onPackClick(effect.packId)
                is CreatorProfileEffect.ShowMessage -> snackbarHostState.showMessage(effect.message.resolveOrDefault())
            }
        }
    }

    CreatorProfileScreen(
        userId = userId,
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}
