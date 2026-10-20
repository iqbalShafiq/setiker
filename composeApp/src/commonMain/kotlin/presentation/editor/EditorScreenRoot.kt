package presentation.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import util.AppLog

@Composable
fun EditorScreenRoot(
    stickerIndex: Int?,
    packId: String,
    croppedImagePath: String? = null,
    onResultProcessed: () -> Unit = {},
    onBackClick: () -> Unit,
    onNavigateToCrop: (String) -> Unit,
    onStickerSaved: () -> Unit,
    viewModel: EditorViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCelebration by remember { mutableStateOf(false) }

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
            AppLog.d("EditorScreenRoot", "Processing crop result: $path")
            viewModel.onIntent(EditorIntent.UpdateImagePath(path))
            onResultProcessed()
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EditorEffect.StickerSaved -> {
                    // Let the celebration land before leaving: saving a sticker is the moment the
                    // whole app has been building towards.
                    showCelebration = true
                    delay(CELEBRATION_DURATION_MS)
                    onStickerSaved()
                }
                is EditorEffect.NavigateBack -> onBackClick()
                is EditorEffect.NavigateToCrop -> onNavigateToCrop(effect.imagePath)
                is EditorEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
                is EditorEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
            }
        }
    }

    EditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        showCelebration = showCelebration
    )
}

/** How long the save celebration stays on screen. */
private const val CELEBRATION_DURATION_MS = 850L
