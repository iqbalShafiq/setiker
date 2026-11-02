package presentation.createpack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState
import presentation.components.rememberAppHaptics
import presentation.theme.SetikerMotion

@Composable
fun CreatePackScreenRoot(
    packId: String?,
    workspaceDraftId: String? = null,
    onBackClick: () -> Unit,
    onPackSaved: (String) -> Unit,
    croppedStickerGalleryPath: String? = null,
    croppedTrayGalleryPath: String? = null,
    pendingAnimatedDraft: DraftSticker? = null,
    onStickerGalleryCropConsumed: () -> Unit = {},
    onTrayGalleryCropConsumed: () -> Unit = {},
    onAnimatedDraftConsumed: () -> Unit = {},
    onNavigateToCropSticker: (String) -> Unit = {},
    onNavigateToCropTray: (String) -> Unit = {},
    onNavigateToVideoTrim: (String) -> Unit = {},
    onNavigateToPublicPack: (String) -> Unit = {},
    viewModel: CreatePackViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }
    val haptics = rememberAppHaptics()
    val snackbarScope = rememberCoroutineScope()
    var showCelebration by remember { mutableStateOf(false) }

    LaunchedEffect(packId) {
        packId?.let { viewModel.onIntent(CreatePackIntent.LoadPack(it)) }
    }

    LaunchedEffect(workspaceDraftId) {
        workspaceDraftId?.let { viewModel.onIntent(CreatePackIntent.RestoreWorkspaceDraft(it)) }
    }

    LaunchedEffect(croppedStickerGalleryPath) {
        if (!croppedStickerGalleryPath.isNullOrBlank()) {
            viewModel.onIntent(CreatePackIntent.AddSticker(croppedStickerGalleryPath))
            onStickerGalleryCropConsumed()
        }
    }

    LaunchedEffect(croppedTrayGalleryPath) {
        if (!croppedTrayGalleryPath.isNullOrBlank()) {
            viewModel.onIntent(CreatePackIntent.UpdateTrayImage(croppedTrayGalleryPath))
            onTrayGalleryCropConsumed()
        }
    }

    LaunchedEffect(pendingAnimatedDraft) {
        pendingAnimatedDraft?.let { draft ->
            viewModel.onIntent(CreatePackIntent.AddAnimatedDraft(draft))
            onAnimatedDraftConsumed()
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreatePackEffect.PackSaved -> {
                    haptics.confirm()
                    showCelebration = true
                    delay(SetikerMotion.CELEBRATION_HOLD_MS.milliseconds)
                    showCelebration = false
                    onPackSaved(effect.packId)
                }
                is CreatePackEffect.PackPublished -> {
                    haptics.confirm()
                    showCelebration = true
                    // The snackbar suspends until it is dismissed. It must not hold the celebration
                    // on screen for its whole ten-second lifetime.
                    snackbarScope.launch { snackbarHostState.showMessage(effect.message) }
                    delay(SetikerMotion.CELEBRATION_HOLD_MS.milliseconds)
                    showCelebration = false
                }
                is CreatePackEffect.NavigateToPublicPack -> onNavigateToPublicPack(effect.cloudPackId)
                is CreatePackEffect.ShowSuccess -> snackbarHostState.showMessage(effect.message)
                is CreatePackEffect.NavigateBack -> onBackClick()
                is CreatePackEffect.ShowError -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
            }
        }
    }

    CreatePackScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onNavigateToCropSticker = onNavigateToCropSticker,
        onNavigateToCropTray = onNavigateToCropTray,
        onNavigateToVideoTrim = onNavigateToVideoTrim,
        onPreviewPublicPack = onNavigateToPublicPack,
        snackbarHostState = snackbarHostState,
        showCelebration = showCelebration
    )
}
