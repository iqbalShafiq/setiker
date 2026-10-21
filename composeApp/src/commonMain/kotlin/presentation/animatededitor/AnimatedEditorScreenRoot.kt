package presentation.animatededitor

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveLocal
import presentation.common.resolveOrDefault

@Composable
fun AnimatedEditorScreenRoot(
    draftId: String,
    packId: String,
    onBackClick: () -> Unit,
    onAnimatedDraftReady: (AnimatedEditorEffect.AnimatedDraftReady) -> Unit,
    viewModel: AnimatedEditorViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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

    // The error of the screen itself goes into the snackbar as well, so the user does not have
    // to scroll to the bottom of a long form to find out that something failed.
    val errorMessage = state.error?.resolveLocal()
    LaunchedEffect(errorMessage) {
        if (errorMessage == null) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Indefinite)
    }

    AnimatedEditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
