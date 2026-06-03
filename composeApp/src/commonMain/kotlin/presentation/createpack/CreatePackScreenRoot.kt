package presentation.createpack

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault

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
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                is CreatePackEffect.PackSaved -> onPackSaved(effect.packId)
                is CreatePackEffect.NavigateToPublicPack -> onNavigateToPublicPack(effect.cloudPackId)
                is CreatePackEffect.ShowSuccess -> snackbarHostState.showSnackbar(effect.message)
                is CreatePackEffect.NavigateBack -> onBackClick()
                is CreatePackEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
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
        snackbarHostState = snackbarHostState
    )
}
