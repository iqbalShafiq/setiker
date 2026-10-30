package presentation.videocrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import domain.model.AnimatedStickerSpec
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState

@Composable
fun VideoCropScreenRoot(
    videoPath: String,
    spec: AnimatedStickerSpec,
    packId: String,
    onBackClick: () -> Unit,
    onNavigateToAnimatedEditor: (String) -> Unit,
    viewModel: VideoCropViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }

    LaunchedEffect(videoPath, spec, packId) {
        viewModel.onIntent(VideoCropIntent.Load(videoPath, spec, packId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VideoCropEffect.NavigateToAnimatedEditor -> onNavigateToAnimatedEditor(effect.draftId)
                is VideoCropEffect.NavigateBack -> onBackClick()
                is VideoCropEffect.ShowError -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
            }
        }
    }

    VideoCropScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
