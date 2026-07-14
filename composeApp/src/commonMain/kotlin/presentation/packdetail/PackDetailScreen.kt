package presentation.packdetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.common.ContentStateAnimations
import presentation.common.DetailLoadPhase
import presentation.common.toShareLinkUi
import presentation.components.AppIllustration
import presentation.components.AppDialog
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.AppTopBarActionIcon
import presentation.components.BottomSheetScrollColumn
import presentation.components.ContentPolicyBottomSheet
import presentation.components.zeroBottomSheetWindowInsets
import presentation.components.DuplicatePackConfirmDialog
import presentation.components.EmptyState
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalSelectableChip
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.ShareCollaboratorUi
import presentation.components.ShareLinksSheet
import presentation.components.ShareWithUsersSheet
import presentation.components.StickerCard
import presentation.components.SyncStatusIndicator
import presentation.components.rememberMultipleImagePicker
import presentation.theme.NeubrutalCardRadius
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.add_to_whatsapp
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.delete
import setiker.composeapp.generated.resources.delete_pack
import setiker.composeapp.generated.resources.pack_collaborators
import setiker.composeapp.generated.resources.pack_duplicate
import setiker.composeapp.generated.resources.pack_duplicating
import setiker.composeapp.generated.resources.pack_make_public
import setiker.composeapp.generated.resources.pack_make_public_dialog_message
import setiker.composeapp.generated.resources.pack_make_public_dialog_title
import setiker.composeapp.generated.resources.pack_publish_confirm
import setiker.composeapp.generated.resources.pack_unpublish
import setiker.composeapp.generated.resources.pack_unpublish_confirm
import setiker.composeapp.generated.resources.pack_unpublish_dialog_message
import setiker.composeapp.generated.resources.pack_unpublish_dialog_title
import setiker.composeapp.generated.resources.pack_updating_visibility
import setiker.composeapp.generated.resources.delete_pack_dialog_message
import setiker.composeapp.generated.resources.delete_pack_dialog_title
import setiker.composeapp.generated.resources.delete_sticker_dialog_message
import setiker.composeapp.generated.resources.delete_sticker_dialog_title
import setiker.composeapp.generated.resources.deleting
import setiker.composeapp.generated.resources.edit_pack
import setiker.composeapp.generated.resources.ic_whatsapp
import setiker.composeapp.generated.resources.info_tray_icon_missing
import setiker.composeapp.generated.resources.import_crop_sheet_message_detail
import setiker.composeapp.generated.resources.import_crop_sheet_primary
import setiker.composeapp.generated.resources.import_crop_sheet_title
import setiker.composeapp.generated.resources.loading_pack
import setiker.composeapp.generated.resources.no_stickers_desc
import setiker.composeapp.generated.resources.no_stickers_title
import setiker.composeapp.generated.resources.pack_details_title
import setiker.composeapp.generated.resources.pack_not_found_desc
import setiker.composeapp.generated.resources.pack_not_found_title
import setiker.composeapp.generated.resources.pack_stickers_hint
import setiker.composeapp.generated.resources.processing
import setiker.composeapp.generated.resources.reorder_drag_handle
import setiker.composeapp.generated.resources.reorder_move_down
import setiker.composeapp.generated.resources.reorder_move_up
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import setiker.composeapp.generated.resources.share_pack
import setiker.composeapp.generated.resources.sticker_links_title
import setiker.composeapp.generated.resources.sticker_share_tab_links
import setiker.composeapp.generated.resources.sticker_share_tab_people
import setiker.composeapp.generated.resources.sticker_preview_content_description
import setiker.composeapp.generated.resources.stickers_title
import setiker.composeapp.generated.resources.stickers_with_count
import setiker.composeapp.generated.resources.tray_icon_content_description
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackDetailScreen(
    state: PackDetailState,
    onIntent: (PackDetailIntent) -> Unit,
    onBackClick: () -> Unit,
    onEditPack: () -> Unit,
    onAddSticker: () -> Unit,
    onEditSticker: (Int) -> Unit,
    onNavigateToCropStickerImport: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteStickerImageFile by remember { mutableStateOf<String?>(null) }
    val isOperationInProgress = state.isDeleting ||
        state.cloudShareLinksLoading ||
        state.isDuplicating ||
        state.isUpdatingVisibility
    val bottomOperationLabel = when {
        state.isDeleting -> stringResource(Res.string.deleting)
        state.isDuplicating -> stringResource(Res.string.pack_duplicating)
        state.isUpdatingVisibility -> stringResource(Res.string.pack_updating_visibility)
        state.cloudShareLinksLoading -> stringResource(Res.string.processing)
        else -> null
    }
    val isPublicPack = state.pack?.visibility.equals("PUBLIC", ignoreCase = true)
    val topBarActionsEnabled = !state.isLoading && !isOperationInProgress && state.pack != null

    val multipleImagePicker = rememberMultipleImagePicker { imagePaths ->
        if (imagePaths.isNotEmpty()) {
            onIntent(PackDetailIntent.StageStickerImports(imagePaths))
        }
    }

    val stickerImportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (state.stickerImportQueue.isNotEmpty()) {
        val importPath = state.stickerImportQueue.first()
        val queueSize = state.stickerImportQueue.size
        ModalBottomSheet(
            onDismissRequest = { onIntent(PackDetailIntent.DismissStickerImportSheet) },
            sheetState = stickerImportSheetState,
            containerColor = neubrutalScreenBackground(),
            scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.import_crop_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = neubrutalOnSurface()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        Res.string.import_crop_sheet_message_detail,
                        1,
                        queueSize
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(NeubrutalCardRadius))
                        .background(neubrutalCardSurface())
                        .neubrutalBorderWithGloss(
                            color = neubrutalBorderColor(),
                            cornerRadius = NeubrutalCardRadius,
                            highlightColor = neubrutalGlossyHighlightColor()
                        )
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = importPath,
                        contentDescription = stringResource(Res.string.sticker_preview_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                AppPrimaryButton(
                    text = stringResource(Res.string.import_crop_sheet_primary),
                    onClick = { onNavigateToCropStickerImport(importPath) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(PackDetailIntent.DismissStickerImportSheet) }
                )
            }
        }
    }

    state.visibilityDialog?.let { dialog ->
        val makePublic = dialog == VisibilityDialog.MakePublic
        AppDialog(
            title = stringResource(
                if (makePublic) {
                    Res.string.pack_make_public_dialog_title
                } else {
                    Res.string.pack_unpublish_dialog_title
                }
            ),
            message = stringResource(
                if (makePublic) {
                    Res.string.pack_make_public_dialog_message
                } else {
                    Res.string.pack_unpublish_dialog_message
                }
            ),
            confirmText = stringResource(
                if (makePublic) {
                    Res.string.pack_publish_confirm
                } else {
                    Res.string.pack_unpublish_confirm
                }
            ),
            onConfirm = { onIntent(PackDetailIntent.ConfirmVisibilityChange) },
            onDismiss = { onIntent(PackDetailIntent.DismissVisibilityDialog) },
            isDanger = false,
            confirmEnabled = !state.isUpdatingVisibility
        )
    }

    ContentPolicyBottomSheet(
        visible = state.showContentPolicySheet,
        onDismiss = { onIntent(PackDetailIntent.DismissContentPolicy) },
        onAccept = { onIntent(PackDetailIntent.AcceptContentPolicy) }
    )

    if (state.showDuplicateDialog) {
        DuplicatePackConfirmDialog(
            packName = state.pack?.name.orEmpty(),
            onConfirm = { onIntent(PackDetailIntent.ConfirmDuplicate) },
            onDismiss = { onIntent(PackDetailIntent.DismissDuplicateDialog) },
            confirmEnabled = !state.isDuplicating
        )
    }

    if (state.collaboratorsSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(PackDetailIntent.DismissCollaboratorsSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = neubrutalScreenBackground(),
            contentWindowInsets = { zeroBottomSheetWindowInsets() }
        ) {
            BottomSheetScrollColumn {
                PackCollaboratorsSheet(
                    searchQuery = state.collaboratorSearchQuery,
                    onSearchQueryChange = { onIntent(PackDetailIntent.CollaboratorSearchChanged(it)) },
                    searchResults = state.collaboratorSearchResults,
                    collaborators = state.collaborators,
                    isLoading = state.collaboratorsLoading,
                    invitePermission = state.collaboratorInvitePermission,
                    onInvitePermissionChange = { onIntent(PackDetailIntent.CollaboratorPermissionChanged(it)) },
                    onInvite = { onIntent(PackDetailIntent.InviteCollaborator(it)) },
                    onRemove = { onIntent(PackDetailIntent.RemoveCollaborator(it)) },
                    onRefresh = { onIntent(PackDetailIntent.RefreshCollaborators) }
                )
            }
        }
    }

    if (state.cloudShareSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(PackDetailIntent.DismissCloudShareSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = neubrutalScreenBackground()
        ) {
            ShareLinksSheet(
                isLoading = state.cloudShareLinksLoading,
                links = state.cloudShareLinks.map { it.toShareLinkUi() },
                enabled = !state.cloudShareLinksLoading,
                onRefresh = { onIntent(PackDetailIntent.RefreshCloudShareLinks) },
                onCreate = { onIntent(PackDetailIntent.CreateCloudShareLink) },
                onRevoke = { onIntent(PackDetailIntent.RevokeCloudShareLink(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            )
        }
    }

    if (state.stickerShareSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(PackDetailIntent.DismissStickerShareSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = neubrutalScreenBackground()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeubrutalSelectableChip(
                        label = stringResource(Res.string.sticker_share_tab_links),
                        selected = state.stickerShareTab == StickerShareTab.Links,
                        onClick = {
                            onIntent(PackDetailIntent.StickerShareTabChanged(StickerShareTab.Links))
                        }
                    )
                    NeubrutalSelectableChip(
                        label = stringResource(Res.string.sticker_share_tab_people),
                        selected = state.stickerShareTab == StickerShareTab.People,
                        onClick = {
                            onIntent(PackDetailIntent.StickerShareTabChanged(StickerShareTab.People))
                        }
                    )
                }
                when (state.stickerShareTab) {
                    StickerShareTab.Links -> {
                        ShareLinksSheet(
                            isLoading = state.stickerShareLinksLoading,
                            links = state.stickerShareLinks.map { it.toShareLinkUi() },
                            enabled = !state.stickerShareLinksLoading,
                            onRefresh = { onIntent(PackDetailIntent.RefreshStickerShareLinks) },
                            onCreate = { onIntent(PackDetailIntent.CreateStickerShareLink) },
                            onRevoke = { onIntent(PackDetailIntent.RevokeStickerShareLink(it)) },
                            title = stringResource(Res.string.sticker_links_title),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    StickerShareTab.People -> {
                        ShareWithUsersSheet(
                            searchQuery = state.stickerCollaboratorSearchQuery,
                            onSearchQueryChange = {
                                onIntent(PackDetailIntent.StickerCollaboratorSearchChanged(it))
                            },
                            searchResults = state.stickerCollaboratorSearchResults,
                            collaborators = state.stickerCollaborators.map { collab ->
                                ShareCollaboratorUi(
                                    userId = collab.sharedWithId,
                                    displayLabel = collab.sharedWith?.displayName
                                        ?: collab.sharedWith?.username
                                        ?: collab.sharedWithId,
                                    permission = collab.permission
                                )
                            },
                            isLoading = state.stickerCollaboratorsLoading,
                            invitePermission = state.stickerCollaboratorInvitePermission,
                            onInvitePermissionChange = {
                                onIntent(PackDetailIntent.StickerCollaboratorPermissionChanged(it))
                            },
                            onInvite = { onIntent(PackDetailIntent.InviteStickerCollaborator(it)) },
                            onRemove = { onIntent(PackDetailIntent.RemoveStickerCollaborator(it)) },
                            onRefresh = { onIntent(PackDetailIntent.RefreshStickerCollaborators) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.pack?.name ?: stringResource(Res.string.pack_details_title),
                onBackClick = null,
                actions = {
                    AppTopBarActionIcon(
                        icon = Icons.Default.Group,
                        contentDescription = stringResource(Res.string.pack_collaborators),
                        onClick = { onIntent(PackDetailIntent.OpenCollaboratorsSheet) },
                        enabled = topBarActionsEnabled && !state.pack?.cloudId.isNullOrBlank()
                    )
                    AppTopBarActionIcon(
                        icon = if (isPublicPack) Icons.Default.VisibilityOff else Icons.Default.Public,
                        contentDescription = stringResource(
                            if (isPublicPack) Res.string.pack_unpublish else Res.string.pack_make_public
                        ),
                        onClick = {
                            if (isPublicPack) {
                                onIntent(PackDetailIntent.RequestUnpublish)
                            } else {
                                onIntent(PackDetailIntent.RequestMakePublic)
                            }
                        },
                        enabled = topBarActionsEnabled
                    )
                    AppTopBarActionIcon(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = stringResource(Res.string.pack_duplicate),
                        onClick = { onIntent(PackDetailIntent.RequestDuplicate) },
                        enabled = topBarActionsEnabled
                    )
                }
            )
        },
        bottomBar = {
            PackBottomBar(
                actionStatusText = bottomOperationLabel,
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        onClick = onBackClick,
                        enabled = !state.isLoading && !isOperationInProgress
                    )
                    PackBottomBarIconButton(
                        painter = painterResource(Res.drawable.ic_whatsapp),
                        contentDescription = stringResource(Res.string.add_to_whatsapp),
                        onClick = {
                            state.pack?.let { onIntent(PackDetailIntent.AddToWhatsApp(it.identifier)) }
                        },
                        enabled = !state.isLoading && !isOperationInProgress && state.pack != null,
                        containerColor = androidx.compose.ui.graphics.Color(0xFF25D366),
                        iconTint = androidx.compose.ui.graphics.Color.White
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = stringResource(Res.string.share_pack),
                        onClick = { onIntent(PackDetailIntent.OpenCloudShareSheet) },
                        enabled = !state.isLoading && !isOperationInProgress
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.delete_pack),
                        onClick = { showDeleteDialog = true },
                        enabled = !state.isLoading && !isOperationInProgress
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.edit_pack),
                        onClick = onEditPack,
                        enabled = !state.isLoading && !isOperationInProgress
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        val phase = when {
            state.isLoading -> DetailLoadPhase.Loading
            state.pack == null -> DetailLoadPhase.Failed
            else -> DetailLoadPhase.Ready
        }
        AnimatedContent(
            targetState = phase,
            transitionSpec = { with(ContentStateAnimations) { detailReveal() } },
            label = "pack_detail_phase",
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { current ->
            when (current) {
                DetailLoadPhase.Loading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        label = stringResource(Res.string.loading_pack),
                        illustration = AppIllustration.LoadingState
                    )
                }
                DetailLoadPhase.Failed -> {
                    EmptyState(
                        title = stringResource(Res.string.pack_not_found_title),
                        description = stringResource(Res.string.pack_not_found_desc),
                        illustration = AppIllustration.ErrorState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                DetailLoadPhase.GuestEmpty -> Unit
                DetailLoadPhase.Ready -> {
                    val pack = state.pack ?: return@AnimatedContent
                    InteractionBlockedBox(
                        blocked = isOperationInProgress,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PackDetailContent(
                            pack = pack,
                            enabled = !isOperationInProgress,
                            onIntent = onIntent,
                            onEditSticker = onEditSticker,
                            onDeleteSticker = { deleteStickerImageFile = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AppDialog(
            title = stringResource(Res.string.delete_pack_dialog_title),
            message = stringResource(Res.string.delete_pack_dialog_message, state.pack?.name ?: ""),
            confirmText = stringResource(Res.string.delete),
            onConfirm = {
                state.pack?.let { pack ->
                    onIntent(PackDetailIntent.DeletePack(pack.identifier))
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    deleteStickerImageFile?.let { imageFile ->
        AppDialog(
            title = stringResource(Res.string.delete_sticker_dialog_title),
            message = stringResource(Res.string.delete_sticker_dialog_message),
            confirmText = stringResource(Res.string.delete),
            onConfirm = {
                val index = state.pack?.stickers?.indexOfFirst { it.imageFile == imageFile } ?: -1
                if (index >= 0) {
                    onIntent(PackDetailIntent.DeleteSticker(index))
                }
                deleteStickerImageFile = null
            },
            onDismiss = { deleteStickerImageFile = null }
        )
    }
}

@Composable
private fun PackDetailContent(
    pack: StickerPack,
    enabled: Boolean,
    onIntent: (PackDetailIntent) -> Unit,
    onEditSticker: (Int) -> Unit,
    onDeleteSticker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
    ) {
        // Pack Info Card (Neubrutal)
        val border = neubrutalBorderColor()
        val shadow = neubrutalShadowColor()
        val surface = neubrutalCardSurface()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neubrutalShadow(
                    offsetX = NeubrutalShadowOffset,
                    offsetY = NeubrutalShadowOffset,
                    cornerRadius = NeubrutalCardRadius,
                    color = shadow
                )
                .clip(RoundedCornerShape(NeubrutalCardRadius))
                .background(surface)
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = NeubrutalCardRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val trayModifier = Modifier
                    .size(96.dp)
                    .neubrutalShadow(
                        offsetX = NeubrutalSmallShadowOffset,
                        offsetY = NeubrutalSmallShadowOffset,
                        cornerRadius = NeubrutalCardRadius,
                        color = shadow
                    )
                    .clip(RoundedCornerShape(NeubrutalCardRadius))
                    .background(surface)
                    .neubrutalBorderWithGloss(
                        color = border,
                        cornerRadius = NeubrutalCardRadius,
                        highlightColor = neubrutalGlossyHighlightColor()
                    )
                if (pack.trayImageFile.isBlank()) {
                    Box(
                        modifier = trayModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.tray_icon_content_description),
                            tint = neubrutalMutedOnSurface()
                        )
                    }
                } else {
                    AsyncImage(
                        model = pack.trayImageFile,
                        contentDescription = pack.name,
                        modifier = trayModifier,
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = neubrutalOnSurface()
                    )
                    Text(
                        text = pack.publisher,
                        style = MaterialTheme.typography.bodyMedium,
                        color = neubrutalMutedOnSurface()
                    )
                    if (pack.trayImageFile.isBlank()) {
                        Text(
                            text = stringResource(Res.string.info_tray_icon_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = neubrutalMutedOnSurface(),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = stringResource(Res.string.stickers_with_count, pack.stickers.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface()
                    )
                }
            }
            SyncStatusIndicator(
                syncState = pack.syncState,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stickers Grid
        Text(
            text = stringResource(Res.string.stickers_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface()
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.pack_stickers_hint),
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalMutedOnSurface()
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (pack.stickers.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.no_stickers_title),
                description = stringResource(Res.string.no_stickers_desc),
                illustration = AppIllustration.EmptyPack,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            var orderedStickers by remember { mutableStateOf(pack.stickers) }
            LaunchedEffect(pack.stickers) {
                orderedStickers = pack.stickers
            }
            fun indexInPack(imageFile: String): Int =
                pack.stickers.indexOfFirst { it.imageFile == imageFile }
            val lazyGridState = rememberLazyGridState()
            val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
                if (!enabled || from.index == to.index) return@rememberReorderableLazyGridState
                orderedStickers = orderedStickers.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
                onIntent(PackDetailIntent.ReorderSticker(from.index, to.index))
            }
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .graphicsLayer { clip = false },
                state = lazyGridState,
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
            ) {
                itemsIndexed(
                    items = orderedStickers,
                    key = { _, sticker -> sticker.imageFile }
                ) { index, sticker ->
                    ReorderableItem(
                        reorderableLazyGridState,
                        key = sticker.imageFile,
                        enabled = enabled && orderedStickers.size > 1
                    ) {
                        Column {
                            StickerCard(
                                sticker = sticker,
                                onClick = {
                                    val packIndex = indexInPack(sticker.imageFile)
                                    if (packIndex >= 0) onEditSticker(packIndex)
                                },
                                onLongClick = if (enabled) {
                                    {
                                        val packIndex = indexInPack(sticker.imageFile)
                                        if (packIndex >= 0) {
                                            onIntent(PackDetailIntent.OpenStickerShareSheet(packIndex))
                                        }
                                    }
                                } else {
                                    null
                                },
                                onDeleteClick = if (enabled) {
                                    { onDeleteSticker(sticker.imageFile) }
                                } else {
                                    null
                                },
                                showDecorations = false,
                                modifier = Modifier.animateItem()
                            )
                            if (enabled && orderedStickers.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val packIndex = indexInPack(sticker.imageFile)
                                            if (packIndex > 0) {
                                                onIntent(PackDetailIntent.MoveStickerUp(packIndex))
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = stringResource(Res.string.reorder_move_up)
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier.draggableHandle(),
                                        onClick = {}
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = stringResource(Res.string.reorder_drag_handle)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val packIndex = indexInPack(sticker.imageFile)
                                            if (packIndex >= 0 && packIndex < pack.stickers.lastIndex) {
                                                onIntent(PackDetailIntent.MoveStickerDown(packIndex))
                                            }
                                        },
                                        enabled = index < orderedStickers.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(Res.string.reorder_move_down)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Previews

private val mockPack = StickerPack(
    identifier = "pack_preview",
    name = "Funny Cats",
    publisher = "CatLover",
    trayImageFile = "",
    stickers = listOf(
        Sticker(imageFile = "", emojis = listOf("😂", "🐱")),
        Sticker(imageFile = "", emojis = listOf("😻")),
        Sticker(imageFile = "", emojis = listOf("🙀", "❤️")),
        Sticker(imageFile = "", emojis = listOf("😹"))
    )
)

@Preview
@Composable
private fun PackDetailScreenLoadingPreview() {
    MaterialTheme {
        PackDetailScreen(
            state = PackDetailState(isLoading = true),
            onIntent = {},
            onBackClick = {},
            onEditPack = {},
            onAddSticker = {},
            onEditSticker = {},
            onNavigateToCropStickerImport = {}
        )
    }
}

@Preview
@Composable
private fun PackDetailScreenPreview() {
    MaterialTheme {
        PackDetailScreen(
            state = PackDetailState(pack = mockPack, isLoading = false),
            onIntent = {},
            onBackClick = {},
            onEditPack = {},
            onAddSticker = {},
            onEditSticker = {},
            onNavigateToCropStickerImport = {}
        )
    }
}
