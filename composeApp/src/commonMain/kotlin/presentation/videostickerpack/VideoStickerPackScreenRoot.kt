package presentation.videostickerpack

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

@Composable
fun VideoStickerPackScreenRoot(
    videoPath: String,
    onBackClick: () -> Unit,
    onNavigateToPackDetail: (String) -> Unit,
    viewModel: VideoStickerPackViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(videoPath) {
        viewModel.onIntent(VideoStickerPackIntent.LoadVideo(videoPath))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VideoStickerPackEffect.NavigateToPackDetail -> onNavigateToPackDetail(effect.packId)
                is VideoStickerPackEffect.NavigateBack -> onBackClick()
                is VideoStickerPackEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
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
