package presentation.videotrim

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
fun VideoTrimScreenRoot(
    videoPath: String,
    onBackClick: () -> Unit,
    onNavigateToVideoCrop: (String, AnimatedStickerSpec) -> Unit,
    viewModel: VideoTrimViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(videoPath) {
        viewModel.onIntent(VideoTrimIntent.LoadVideo(videoPath))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VideoTrimEffect.NavigateToVideoCrop -> onNavigateToVideoCrop(effect.videoPath, effect.spec)
                is VideoTrimEffect.NavigateBack -> onBackClick()
                is VideoTrimEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
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
