package presentation.videocrop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import domain.model.AnimatedStickerSpec
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun VideoCropScreenRoot(
    videoPath: String,
    spec: AnimatedStickerSpec,
    packId: String,
    onBackClick: () -> Unit,
    onNavigateToAnimatedEditor: (String) -> Unit,
    viewModel: VideoCropViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(videoPath, spec, packId) {
        viewModel.onIntent(VideoCropIntent.Load(videoPath, spec, packId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VideoCropEffect.NavigateToAnimatedEditor -> onNavigateToAnimatedEditor(effect.draftId)
                is VideoCropEffect.NavigateBack -> onBackClick()
                is VideoCropEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
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
