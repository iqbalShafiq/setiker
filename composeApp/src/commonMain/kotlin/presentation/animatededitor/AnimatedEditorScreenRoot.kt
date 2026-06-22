package presentation.animatededitor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun AnimatedEditorScreenRoot(
    draftId: String,
    packId: String,
    onBackClick: () -> Unit,
    onAnimatedDraftReady: (AnimatedEditorEffect.AnimatedDraftReady) -> Unit,
    viewModel: AnimatedEditorViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(packId) {
        viewModel.onIntent(AnimatedEditorIntent.SetPackId(packId))
    }

    LaunchedEffect(draftId) {
        viewModel.onIntent(AnimatedEditorIntent.LoadDraft(draftId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AnimatedEditorEffect.NavigateBack -> onBackClick()
                is AnimatedEditorEffect.AnimatedDraftReady -> onAnimatedDraftReady(effect)
                is AnimatedEditorEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
            }
        }
    }

    AnimatedEditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
