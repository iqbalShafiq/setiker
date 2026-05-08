package presentation.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import presentation.common.resolve

@Composable
fun EditorScreenRoot(
    stickerIndex: Int?,
    packId: String,
    croppedImagePath: String? = null,
    removedBgImagePath: String? = null,
    onResultProcessed: () -> Unit = {},
    onBackClick: () -> Unit,
    onNavigateToCrop: (String) -> Unit,
    onNavigateToBackgroundRemover: (String) -> Unit,
    onStickerSaved: () -> Unit,
    viewModel: EditorViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Always set packId and stickerIndex when entering the screen or recomposing.
    // This ensures both are set even when returning from crop/bg screens
    // where the ViewModel may have been recreated.
    LaunchedEffect(packId, stickerIndex) {
        viewModel.onIntent(EditorIntent.SetPackId(packId, stickerIndex))
    }

    LaunchedEffect(stickerIndex, packId) {
        // Only load sticker data if we're not processing a crop/bg result.
        // This prevents the original sticker from overwriting the edited image path
        // when EditorScreenRoot re-enters composition after returning from crop/bg screens.
        if (croppedImagePath == null && removedBgImagePath == null) {
            stickerIndex?.let { viewModel.onIntent(EditorIntent.LoadSticker(it, packId)) }
        }
    }

    // Handle results from crop or background remover
    LaunchedEffect(croppedImagePath, removedBgImagePath) {
        var processed = false

        croppedImagePath?.let { path ->
            android.util.Log.d("EditorScreenRoot", "Processing crop result: $path")
            viewModel.onIntent(EditorIntent.UpdateImagePath(path))
            processed = true
        }

        removedBgImagePath?.let { path ->
            android.util.Log.d("EditorScreenRoot", "Processing BG remove result: $path")
            viewModel.onIntent(EditorIntent.UpdateImagePath(path))
            processed = true
        }

        if (processed) {
            onResultProcessed()
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EditorEffect.StickerSaved -> onStickerSaved()
                is EditorEffect.NavigateBack -> onBackClick()
                is EditorEffect.NavigateToCrop -> onNavigateToCrop(effect.imagePath)
                is EditorEffect.NavigateToBackgroundRemover -> onNavigateToBackgroundRemover(effect.imagePath)
                is EditorEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolve())
                }
            }
        }
    }

    EditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
