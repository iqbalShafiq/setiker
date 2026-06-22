package presentation.creator

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun CreatorProfileScreenRoot(
    userId: String,
    onBackClick: () -> Unit,
    onPackClick: (String) -> Unit,
    viewModel: CreatorProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.onIntent(CreatorProfileIntent.Load(userId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CreatorProfileEffect.NavigateBack -> onBackClick()
                is CreatorProfileEffect.NavigateToPack -> onPackClick(effect.packId)
                is CreatorProfileEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
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
