package presentation.videostickerpack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState

@Composable
fun VideoStickerPackScreenRoot(
    videoPath: String,
    onBackClick: () -> Unit,
    onNavigateToPackDetail: (String) -> Unit,
    viewModel: VideoStickerPackViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }

    LaunchedEffect(videoPath) {
        viewModel.onIntent(VideoStickerPackIntent.LoadVideo(videoPath))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VideoStickerPackEffect.NavigateToPackDetail -> onNavigateToPackDetail(effect.packId)
                is VideoStickerPackEffect.NavigateBack -> onBackClick()
                is VideoStickerPackEffect.ShowError -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
            }
        }
    }

    VideoStickerPackScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
