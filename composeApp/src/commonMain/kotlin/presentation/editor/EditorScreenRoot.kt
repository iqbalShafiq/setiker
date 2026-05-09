package presentation.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import presentation.common.resolveOrDefault

@Composable
fun EditorScreenRoot(
    stickerIndex: Int?,
    packId: String,
    croppedImagePath: String? = null,
    onResultProcessed: () -> Unit = {},
    onBackClick: () -> Unit,
    onNavigateToCrop: (String) -> Unit,
    onStickerSaved: () -> Unit,
    viewModel: EditorViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Always set packId and stickerIndex when entering the screen or recomposing.
    // This ensures both are set even when returning from crop
    // where the ViewModel may have been recreated.
    LaunchedEffect(packId, stickerIndex) {
        viewModel.onIntent(EditorIntent.SetPackId(packId, stickerIndex))
    }

    LaunchedEffect(stickerIndex, packId) {
        // Only load sticker data if we're not processing a crop result.
        // This prevents the original sticker from overwriting the edited image path
        // when EditorScreenRoot re-enters composition after returning from crop.
        if (croppedImagePath == null) {
            stickerIndex?.let { viewModel.onIntent(EditorIntent.LoadSticker(it, packId)) }
        }
    }

    LaunchedEffect(croppedImagePath) {
        croppedImagePath?.let { path ->
            android.util.Log.d("EditorScreenRoot", "Processing crop result: $path")
            viewModel.onIntent(EditorIntent.UpdateImagePath(path))
            onResultProcessed()
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EditorEffect.StickerSaved -> onStickerSaved()
                is EditorEffect.NavigateBack -> onBackClick()
                is EditorEffect.NavigateToCrop -> onNavigateToCrop(effect.imagePath)
                is EditorEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
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
