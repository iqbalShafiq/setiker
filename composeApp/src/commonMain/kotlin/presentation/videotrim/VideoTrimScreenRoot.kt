package presentation.videotrim

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
fun VideoTrimScreenRoot(
    videoPath: String,
    onBackClick: () -> Unit,
    onNavigateToVideoCrop: (String, AnimatedStickerSpec) -> Unit,
    viewModel: VideoTrimViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }

    LaunchedEffect(videoPath) {
        viewModel.onIntent(VideoTrimIntent.LoadVideo(videoPath))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VideoTrimEffect.NavigateToVideoCrop -> onNavigateToVideoCrop(effect.videoPath, effect.spec)
                is VideoTrimEffect.NavigateBack -> onBackClick()
                is VideoTrimEffect.ShowError -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
            }
        }
    }

    VideoTrimScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
