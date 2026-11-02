package presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState
import presentation.components.rememberAppHaptics
import presentation.theme.SetikerMotion
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.sticker_saved
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
    val snackbarHostState = remember { AppSnackbarHostState() }
    val haptics = rememberAppHaptics()
    val stickerSavedMessage = stringResource(Res.string.sticker_saved)

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
                    // Let the confirmation land before leaving: saving a sticker is the moment the
                    // whole app has been building towards. The snackbar owns the confirmation, so
                    // the screen does not need a second, button-shaped overlay.
                    haptics.confirm()
                    launch { snackbarHostState.showMessage(stickerSavedMessage) }
                    delay(SetikerMotion.CONFIRMATION_HOLD_MS.milliseconds)
                    onStickerSaved()
                }
                is EditorEffect.NavigateBack -> onBackClick()
                is EditorEffect.NavigateToCrop -> onNavigateToCrop(effect.imagePath)
                is EditorEffect.ShowError -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
                is EditorEffect.ShowMessage -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
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
