package presentation.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

@Composable
fun EditorScreenRoot(
    stickerIndex: Int?,
    packId: String,
    onBackClick: () -> Unit,
    onNavigateToCrop: (String) -> Unit,
    onNavigateToBackgroundRemover: (String) -> Unit,
    onStickerSaved: () -> Unit,
    viewModel: EditorViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(stickerIndex, packId) {
        stickerIndex?.let { viewModel.onIntent(EditorIntent.LoadSticker(it, packId)) }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EditorEffect.StickerSaved -> onStickerSaved()
                is EditorEffect.NavigateBack -> onBackClick()
                is EditorEffect.NavigateToCrop -> onNavigateToCrop(effect.imagePath)
                is EditorEffect.NavigateToBackgroundRemover -> onNavigateToBackgroundRemover(effect.imagePath)
                is EditorEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
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
