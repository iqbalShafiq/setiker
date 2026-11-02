package presentation.animatededitor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveLocal
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState
import presentation.components.rememberAppHaptics
import presentation.theme.SetikerMotion

@Composable
fun AnimatedEditorScreenRoot(
    draftId: String,
    packId: String,
    onBackClick: () -> Unit,
    onAnimatedDraftReady: (AnimatedEditorEffect.AnimatedDraftReady) -> Unit,
    viewModel: AnimatedEditorViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }
    val haptics = rememberAppHaptics()
    var showCelebration by remember { mutableStateOf(false) }

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
                is AnimatedEditorEffect.AnimatedDraftReady -> {
                    haptics.confirm()
                    showCelebration = true
                    delay(SetikerMotion.CELEBRATION_HOLD_MS.milliseconds)
                    showCelebration = false
                    onAnimatedDraftReady(effect)
                }
                is AnimatedEditorEffect.ShowError -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
            }
        }
    }

    // The error of the screen itself goes into the snackbar as well, so the user does not have
    // to scroll to the bottom of a long form to find out that something failed.
    val errorMessage = state.error?.resolveLocal()
    LaunchedEffect(errorMessage) {
        if (errorMessage == null) return@LaunchedEffect
        snackbarHostState.hostState.currentSnackbarData?.dismiss()
        snackbarHostState.showError(errorMessage)
    }

    AnimatedEditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        showCelebration = showCelebration
    )
}
