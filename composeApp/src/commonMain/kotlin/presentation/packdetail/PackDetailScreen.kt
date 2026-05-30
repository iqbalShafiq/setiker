package presentation.packdetail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import data.remote.model.CloudStickerPackShareLink
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.common.rememberShareTextAction
import presentation.components.AppDialog
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
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
import setiker.composeapp.generated.resources.cloud_links_copy
import setiker.composeapp.generated.resources.cloud_links_create
import setiker.composeapp.generated.resources.cloud_links_empty
import setiker.composeapp.generated.resources.cloud_links_refresh
import setiker.composeapp.generated.resources.cloud_links_revoke
import setiker.composeapp.generated.resources.cloud_links_share
import setiker.composeapp.generated.resources.cloud_links_title
import setiker.composeapp.generated.resources.delete
import setiker.composeapp.generated.resources.delete_pack
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
import setiker.composeapp.generated.resources.share_pack
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
    var deleteStickerIndex by remember { mutableStateOf(-1) }
    val isOperationInProgress = state.isDeleting || state.cloudShareLinksLoading
    val bottomOperationLabel = when {
        state.isDeleting -> stringResource(Res.string.deleting)
        state.cloudShareLinksLoading -> stringResource(Res.string.processing)
        else -> null
    }

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

    if (state.cloudShareSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(PackDetailIntent.DismissCloudShareSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = neubrutalScreenBackground()
        ) {
            CloudShareLinksSheet(
                isLoading = state.cloudShareLinksLoading,
                links = state.cloudShareLinks,
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.pack?.name ?: stringResource(Res.string.pack_details_title),
                onBackClick = null
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
        when {
            state.isLoading -> {
                LoadingIndicator(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    label = stringResource(Res.string.loading_pack)
                )
            }
            state.pack == null -> {
                EmptyState(
                    title = stringResource(Res.string.pack_not_found_title),
                    description = stringResource(Res.string.pack_not_found_desc),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                InteractionBlockedBox(
                    blocked = isOperationInProgress,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    PackDetailContent(
                        pack = state.pack,
                        enabled = !isOperationInProgress,
                        onIntent = onIntent,
                        onEditSticker = onEditSticker,
                        modifier = Modifier.fillMaxSize()
                    )
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

    if (deleteStickerIndex >= 0) {
        AppDialog(
            title = stringResource(Res.string.delete_sticker_dialog_title),
            message = stringResource(Res.string.delete_sticker_dialog_message),
            confirmText = stringResource(Res.string.delete),
            onConfirm = {
                onIntent(PackDetailIntent.DeleteSticker(deleteStickerIndex))
                deleteStickerIndex = -1
            },
            onDismiss = { deleteStickerIndex = -1 }
        )
    }
}

@Composable
private fun PackDetailContent(
    pack: StickerPack,
    enabled: Boolean,
    onIntent: (PackDetailIntent) -> Unit,
    onEditSticker: (Int) -> Unit,
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
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .graphicsLayer { clip = false },
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
            ) {
                itemsIndexed(
                    items = pack.stickers,
                    key = { index, _ -> index }
                ) { index, sticker ->
                    StickerCard(
                        sticker = sticker,
                        onClick = { onEditSticker(index) },
                        onDeleteClick = { onIntent(PackDetailIntent.DeleteSticker(index)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudShareLinksSheet(
    isLoading: Boolean,
    links: List<CloudStickerPackShareLink>,
    enabled: Boolean,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onRevoke: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val shareText = rememberShareTextAction()
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.cloud_links_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppPrimaryButton(
            text = stringResource(Res.string.cloud_links_create),
            onClick = onCreate,
            enabled = enabled
        )
        Spacer(modifier = Modifier.height(8.dp))
        AppSecondaryButton(
            text = stringResource(Res.string.cloud_links_refresh),
            onClick = onRefresh,
            enabled = enabled
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (isLoading) {
            LoadingIndicator(label = "Loading links")
            return
        }
        if (links.isEmpty()) {
            Text(
                text = stringResource(Res.string.cloud_links_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalMutedOnSurface()
            )
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            links.forEach { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NeubrutalCardRadius))
                        .background(neubrutalCardSurface())
                        .neubrutalBorderWithGloss(
                            color = neubrutalBorderColor(),
                            cornerRadius = NeubrutalCardRadius,
                            highlightColor = neubrutalGlossyHighlightColor()
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = link.shareUrl ?: link.token,
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalOnSurface(),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                        val value = link.shareUrl ?: link.token
                        clipboard.setText(AnnotatedString(value))
                        },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(Res.string.cloud_links_copy)
                        )
                    }
                    IconButton(
                        onClick = {
                        val value = link.shareUrl ?: link.token
                        shareText(value)
                        },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.cloud_links_share)
                        )
                    }
                    IconButton(
                        onClick = { onRevoke(link.id) },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.cloud_links_revoke)
                        )
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
