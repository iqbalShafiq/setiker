package presentation.createpack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.SelectableStickerGrid
import presentation.components.ReadOnlyDecorationOverlay
import presentation.components.ScreenSectionTitle
import presentation.aijob.AiResultSheetVisibility
import presentation.components.AiGenerateBottomSheet
import presentation.components.ImproveConfirmDialog
import presentation.components.BottomSheetScrollColumn
import presentation.components.zeroBottomSheetWindowInsets
import presentation.components.rememberImagePicker
import presentation.components.rememberStickerImagePicker
import presentation.theme.AccentCoral
import presentation.theme.AccentCoralLight
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.add_sticker
import setiker.composeapp.generated.resources.add_to_pack
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.close
import setiker.composeapp.generated.resources.cd_add_animated_sticker
import setiker.composeapp.generated.resources.create_pack_title
import setiker.composeapp.generated.resources.edit_pack_title
import setiker.composeapp.generated.resources.generate
import setiker.composeapp.generated.resources.generate_ai
import setiker.composeapp.generated.resources.generate_ai_sheet_title
import setiker.composeapp.generated.resources.generate_confirm_subtitle
import setiker.composeapp.generated.resources.generate_confirm_title
import setiker.composeapp.generated.resources.generate_improve_confirm_title
import setiker.composeapp.generated.resources.generate_tip
import setiker.composeapp.generated.resources.generating
import setiker.composeapp.generated.resources.grid_confirm_hint
import setiker.composeapp.generated.resources.grid_confirm_title
import setiker.composeapp.generated.resources.grid_source_content_description
import setiker.composeapp.generated.resources.grid_split
import setiker.composeapp.generated.resources.import_crop_sheet_message_sticker
import setiker.composeapp.generated.resources.import_crop_sheet_message_tray
import setiker.composeapp.generated.resources.import_crop_sheet_primary
import setiker.composeapp.generated.resources.import_crop_sheet_title
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder
import setiker.composeapp.generated.resources.processing
import setiker.composeapp.generated.resources.prompt_label
import setiker.composeapp.generated.resources.prompt_placeholder
import setiker.composeapp.generated.resources.publisher_label
import setiker.composeapp.generated.resources.publisher_placeholder
import setiker.composeapp.generated.resources.improve_confirm_message_pack
import setiker.composeapp.generated.resources.improve_stickers
import setiker.composeapp.generated.resources.remove_sticker
import setiker.composeapp.generated.resources.replace_stickers
import setiker.composeapp.generated.resources.save_pack
import setiker.composeapp.generated.resources.saving
import setiker.composeapp.generated.resources.selected_count
import setiker.composeapp.generated.resources.select_tray_icon
import setiker.composeapp.generated.resources.split_grid
import setiker.composeapp.generated.resources.split_result_title
import setiker.composeapp.generated.resources.sticker_limit_hint
import setiker.composeapp.generated.resources.sticker_preview_content_description
import setiker.composeapp.generated.resources.stickers_count
import setiker.composeapp.generated.resources.tray_icon
import setiker.composeapp.generated.resources.tray_icon_content_description
import setiker.composeapp.generated.resources.update_pack
import setiker.composeapp.generated.resources.updating
import setiker.composeapp.generated.resources.visibility_label
import setiker.composeapp.generated.resources.visibility_private
import setiker.composeapp.generated.resources.visibility_public

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePackScreen(
    state: CreatePackState,
    onIntent: (CreatePackIntent) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToCropSticker: (String) -> Unit,
    onNavigateToCropTray: (String) -> Unit,
    onNavigateToVideoTrim: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val trayIconPicker = rememberImagePicker { path ->
        path?.let { onIntent(CreatePackIntent.StageTrayGalleryPick(it)) }
    }

    // Tombol "Add": menerima static image dan animated GIF dalam satu picker.
    // GIF diarahkan ke jalur animated (VideoTrim -> VideoCrop -> AnimatedEditor),
    // sama persis dengan tombol movie di bottom action bar.
    val stickerPicker = rememberStickerImagePicker { path, isAnimated ->
        if (path == null) return@rememberStickerImagePicker
        if (isAnimated) {
            onNavigateToVideoTrim(path)
        } else {
            onIntent(CreatePackIntent.StageStickerGalleryPick(path))
        }
    }

    val videoPicker = presentation.components.rememberVideoPicker { path ->
        path?.let { onNavigateToVideoTrim(it) }
    }

    val gridSourcePicker = rememberImagePicker { path ->
        path?.let {
            onIntent(CreatePackIntent.UpdateGridSplitSource(it))
            onIntent(CreatePackIntent.OpenGridConfirmSheet)
        }
    }

    val generateInputImagePicker = rememberImagePicker { path ->
        path?.let { onIntent(CreatePackIntent.UpdateGenerateInputImage(it)) }
    }

    val generatedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gridSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stickerImportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trayImportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isOperationInProgress = state.isApiLoading || state.isSaving
    val canImprovePack = state.stickers.any { !it.isAnimated && it.imagePath.isNotBlank() } && !isOperationInProgress
    val bottomOperationLabel = when {
        state.isSaving && state.isEditing -> stringResource(Res.string.updating)
        state.isSaving -> stringResource(Res.string.saving)
        state.isApiLoading -> state.backgroundJobMessage ?: stringResource(Res.string.processing)
        else -> null
    }

    if (state.aiGenerateSheetOpen) {
        AiGenerateBottomSheet(
            prompt = state.generatePrompt,
            onPromptChange = { onIntent(CreatePackIntent.UpdateGeneratePrompt(it)) },
            inputImagePath = state.generateInputImage,
            onPickInputImage = { generateInputImagePicker.launch() },
            onClearInputImage = { onIntent(CreatePackIntent.UpdateGenerateInputImage(null)) },
            // Pack editor has no inherent "current image" context — the reference image is purely
            // optional. We pass `false` so the sheet copy reads as a generic uploader.
            hasContextualDefault = false,
            isGenerating = state.isApiLoading,
            onGenerate = { onIntent(CreatePackIntent.GenerateStickers) },
            onDismiss = { onIntent(CreatePackIntent.CloseAiGenerateSheet) }
        )
    }

    if (state.improveConfirmVisible) {
        ImproveConfirmDialog(
            message = stringResource(Res.string.improve_confirm_message_pack),
            onConfirm = { onIntent(CreatePackIntent.ConfirmImprovePackStickers) },
            onDismiss = { onIntent(CreatePackIntent.DismissImproveConfirm) }
        )
    }

    if (
        AiResultSheetVisibility.shouldShowGeneratedResultsSheet(
            hasPreview = state.generatedPreview.isNotEmpty(),
            sheetVisible = state.generatedResultsSheetVisible,
            isAiJobInProgress = state.isApiLoading
        )
    ) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(CreatePackIntent.DismissGeneratedResultsSheet) },
            sheetState = generatedSheetState,
            containerColor = neubrutalScreenBackground(),
            scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
            contentWindowInsets = { zeroBottomSheetWindowInsets() }
        ) {
            BottomSheetScrollColumn {
                Text(
                    text = stringResource(
                        if (state.generatedPreviewMode == GeneratedPreviewMode.ReplacePack) {
                            Res.string.generate_improve_confirm_title
                        } else {
                            Res.string.generate_confirm_title
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = neubrutalOnSurface()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.generate_confirm_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        Res.string.selected_count,
                        state.selectedGeneratedPreview.size,
                        state.generatedPreview.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectableStickerGrid(
                    stickers = state.generatedPreview,
                    selectedIndices = state.selectedGeneratedPreview,
                    onToggle = { onIntent(CreatePackIntent.ToggleGeneratedSelection(it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppPrimaryButton(
                    text = stringResource(
                        if (state.generatedPreviewMode == GeneratedPreviewMode.ReplacePack) {
                            Res.string.replace_stickers
                        } else {
                            Res.string.add_to_pack
                        }
                    ),
                    enabled = state.selectedGeneratedPreview.isNotEmpty(),
                    onClick = {
                        onIntent(
                            if (state.generatedPreviewMode == GeneratedPreviewMode.ReplacePack) {
                                CreatePackIntent.ReplacePackWithGenerated
                            } else {
                                CreatePackIntent.AddSelectedGeneratedToPack
                            }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(CreatePackIntent.CancelGeneratedResults) }
                )
            }
        }
    }

    if (state.gridSplitSheetPhase != GridSplitSheetPhase.Hidden) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(CreatePackIntent.DismissGridSheet) },
            sheetState = gridSheetState,
            containerColor = neubrutalScreenBackground(),
            scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
            contentWindowInsets = { zeroBottomSheetWindowInsets() }
        ) {
            BottomSheetScrollColumn {
                when (state.gridSplitSheetPhase) {
                    GridSplitSheetPhase.ConfirmPick -> {
                        Text(
                            text = stringResource(Res.string.grid_confirm_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = neubrutalOnSurface()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.gridSplitSourcePath.isNotBlank()) {
                            AsyncImage(
                                model = state.gridSplitSourcePath,
                                contentDescription = stringResource(Res.string.grid_source_content_description),
                                modifier = Modifier
                                    .size(160.dp)
                                    .neubrutalShadow(3.dp, 3.dp, 16.dp, neubrutalShadowColor())
                                    .clip(RoundedCornerShape(NeubrutalCardRadius))
                                    .background(neubrutalCardSurface())
                                    .neubrutalBorderWithGloss(
                                        color = neubrutalBorderColor(),
                                        cornerRadius = NeubrutalCardRadius,
                                        highlightColor = neubrutalGlossyHighlightColor()
                                    )
                                    .padding(4.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.grid_confirm_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = neubrutalOnSurface()
                        )
                        if (!state.error.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
                            )
                        }
                        if (state.isApiLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { state.backgroundJobProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.backgroundJobMessage ?: stringResource(Res.string.processing),
                                style = MaterialTheme.typography.bodySmall,
                                color = neubrutalMutedOnSurface()
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        AppPrimaryButton(
                            text = stringResource(if (state.isApiLoading) Res.string.processing else Res.string.split_grid),
                            enabled = !state.isApiLoading && state.gridSplitSourcePath.isNotBlank(),
                            onClick = { onIntent(CreatePackIntent.RunGridSplit) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppSecondaryButton(
                            text = stringResource(Res.string.cancel),
                            onClick = { onIntent(CreatePackIntent.CancelGridSheet) }
                        )
                    }

                    GridSplitSheetPhase.Results -> {
                        Text(
                            text = stringResource(Res.string.split_result_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = neubrutalOnSurface()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                Res.string.selected_count,
                                state.selectedSplitPreview.size,
                                state.splitPreview.size
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = neubrutalMutedOnSurface()
                        )
                        if (!state.error.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectableStickerGrid(
                            stickers = state.splitPreview,
                            selectedIndices = state.selectedSplitPreview,
                            onToggle = { onIntent(CreatePackIntent.ToggleSplitSelection(it)) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AppPrimaryButton(
                            text = stringResource(Res.string.add_to_pack),
                            enabled = state.selectedSplitPreview.isNotEmpty(),
                            onClick = { onIntent(CreatePackIntent.AddSelectedSplitToPack) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppSecondaryButton(
                            text = stringResource(Res.string.cancel),
                            onClick = { onIntent(CreatePackIntent.CancelGridSheet) }
                        )
                    }

                }
            }
        }
    }

    state.pendingStickerGalleryPath?.let { galleryPath ->
        ModalBottomSheet(
            onDismissRequest = { onIntent(CreatePackIntent.DismissStickerGalleryCropPrompt) },
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
                    text = stringResource(Res.string.import_crop_sheet_message_sticker),
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
                        model = galleryPath,
                        contentDescription = stringResource(Res.string.sticker_preview_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                AppPrimaryButton(
                    text = stringResource(Res.string.import_crop_sheet_primary),
                    onClick = {
                        onNavigateToCropSticker(galleryPath)
                        onIntent(CreatePackIntent.DismissStickerGalleryCropPrompt)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(CreatePackIntent.DismissStickerGalleryCropPrompt) }
                )
            }
        }
    }

    state.pendingTrayGalleryPath?.let { galleryPath ->
        ModalBottomSheet(
            onDismissRequest = { onIntent(CreatePackIntent.DismissTrayGalleryCropPrompt) },
            sheetState = trayImportSheetState,
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
                    text = stringResource(Res.string.import_crop_sheet_message_tray),
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
                        model = galleryPath,
                        contentDescription = stringResource(Res.string.tray_icon_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                AppPrimaryButton(
                    text = stringResource(Res.string.import_crop_sheet_primary),
                    onClick = {
                        onNavigateToCropTray(galleryPath)
                        onIntent(CreatePackIntent.DismissTrayGalleryCropPrompt)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(CreatePackIntent.DismissTrayGalleryCropPrompt) }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (state.isEditing) Res.string.edit_pack_title else Res.string.create_pack_title),
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
                        enabled = !isOperationInProgress
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Filled.AutoAwesome,
                        contentDescription = stringResource(Res.string.generate_ai),
                        onClick = {
                            if (state.generatedPreview.isNotEmpty() && !isOperationInProgress) {
                                onIntent(CreatePackIntent.ShowGeneratedResultsSheet)
                            } else {
                                onIntent(CreatePackIntent.OpenAiGenerateSheet)
                            }
                        },
                        enabled = !isOperationInProgress
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Filled.AutoFixHigh,
                        contentDescription = stringResource(Res.string.improve_stickers),
                        onClick = { onIntent(CreatePackIntent.RequestImprovePackStickers) },
                        enabled = canImprovePack
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Filled.ViewModule,
                        contentDescription = stringResource(Res.string.grid_split),
                        onClick = {
                            if (state.splitPreview.isNotEmpty() && !isOperationInProgress) {
                                onIntent(CreatePackIntent.ShowGridSplitResultsSheet)
                            } else {
                                gridSourcePicker.launch()
                            }
                        },
                        enabled = !isOperationInProgress
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Filled.Movie,
                        contentDescription = stringResource(Res.string.cd_add_animated_sticker),
                        onClick = { videoPicker.launch() },
                        enabled = !isOperationInProgress
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Filled.Check,
                        contentDescription = stringResource(
                            if (state.isEditing) Res.string.update_pack else Res.string.save_pack
                        ),
                        onClick = { onIntent(CreatePackIntent.SavePack) },
                        enabled = !isOperationInProgress,
                        isLoading = state.isSaving
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingIndicator(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            InteractionBlockedBox(
                blocked = isOperationInProgress,
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                AppTextField(
                    value = state.name,
                    onValueChange = { onIntent(CreatePackIntent.UpdateName(it)) },
                    label = stringResource(Res.string.pack_name_label),
                    placeholder = stringResource(Res.string.pack_name_placeholder),
                    enabled = !isOperationInProgress
                )

                Spacer(modifier = Modifier.height(20.dp))

                AppTextField(
                    value = state.publisher,
                    onValueChange = { onIntent(CreatePackIntent.UpdatePublisher(it)) },
                    label = stringResource(Res.string.publisher_label),
                    placeholder = stringResource(Res.string.publisher_placeholder),
                    enabled = !isOperationInProgress
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(Res.string.visibility_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = neubrutalOnSurface()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.visibility.equals("PRIVATE", ignoreCase = true),
                        onClick = { onIntent(CreatePackIntent.UpdateVisibility("PRIVATE")) },
                        label = { Text(stringResource(Res.string.visibility_private)) },
                        enabled = !isOperationInProgress
                    )
                    FilterChip(
                        selected = state.visibility.equals("PUBLIC", ignoreCase = true),
                        onClick = { onIntent(CreatePackIntent.UpdateVisibility("PUBLIC")) },
                        label = { Text(stringResource(Res.string.visibility_public)) },
                        enabled = !isOperationInProgress
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.tray_icon),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = neubrutalOnSurface()
                )

                Spacer(modifier = Modifier.height(10.dp))

                TrayIconSelector(
                    imagePath = state.trayImagePath,
                    onClick = { trayIconPicker.launch() },
                    enabled = !isOperationInProgress
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.stickers_count, state.stickers.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = neubrutalOnSurface()
                    )

                    IconButton(
                        onClick = { stickerPicker.launch() },
                        enabled = !isOperationInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.add_sticker),
                            tint = AccentCoral
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.sticker_limit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.stickers.chunked(4).forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEachIndexed { itemIndex, stickerDraft ->
                                val stickerIndex = (rowIndex * 4) + itemIndex
                                StickerPreviewItem(
                                    sticker = stickerDraft,
                                    onRemove = { onIntent(CreatePackIntent.RemoveSticker(stickerIndex)) },
                                    enabled = !isOperationInProgress,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun TrayIconSelector(
    imagePath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    if (imagePath.isNotBlank()) {
        Box(
            modifier = modifier
                .size(96.dp)
                .neubrutalShadow(
                    offsetX = NeubrutalShadowOffset,
                    offsetY = NeubrutalShadowOffset,
                    cornerRadius = NeubrutalCardRadius,
                    color = shadow
                )
                .clip(RoundedCornerShape(NeubrutalCardRadius))
                .background(neubrutalCardSurface())
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = NeubrutalCardRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = stringResource(Res.string.tray_icon_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(96.dp)
                .neubrutalShadow(
                    offsetX = NeubrutalShadowOffset,
                    offsetY = NeubrutalShadowOffset,
                    cornerRadius = NeubrutalCardRadius,
                    color = shadow
                )
                .clip(RoundedCornerShape(NeubrutalCardRadius))
                .background(AccentCoralLight)
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = NeubrutalCardRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.select_tray_icon),
                modifier = Modifier.size(32.dp),
                tint = presentation.theme.NeubrutalDark
            )
        }
    }
}

@Composable
private fun StickerPreviewItem(
    sticker: DraftSticker,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val border = neubrutalBorderColor()
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .neubrutalShadow(
                    offsetX = NeubrutalSmallShadowOffset,
                    offsetY = NeubrutalSmallShadowOffset,
                    cornerRadius = NeubrutalSmallRadius,
                    color = neubrutalShadowColor()
                )
                .clip(RoundedCornerShape(NeubrutalSmallRadius))
                .background(neubrutalCardSurface())
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = NeubrutalSmallRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .padding(2.dp)
        ) {
            AsyncImage(
                model = sticker.imagePath,
                contentDescription = stringResource(Res.string.sticker_preview_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            ReadOnlyDecorationOverlay(
                decorations = sticker.decorations,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ErrorRed)
                .neubrutalBorderWithGloss(
                    color = border,
                    width = 1.5.dp,
                    cornerRadius = 6.dp,
                    highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
                )
                .clickable(enabled = enabled, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.remove_sticker),
                modifier = Modifier.size(14.dp),
                tint = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

@Preview
@Composable
private fun CreatePackScreenPreview() {
    MaterialTheme {
        CreatePackScreen(
            state = CreatePackState(
                name = "My Awesome Pack",
                publisher = "StickerFan",
                trayImagePath = "",
                stickers = listOf(DraftSticker(""), DraftSticker(""), DraftSticker("")),
                isEditing = false
            ),
            onIntent = {},
            onBackClick = {},
            onNavigateToCropSticker = {},
            onNavigateToCropTray = {}
        )
    }
}

@Preview
@Composable
private fun CreatePackScreenEditingPreview() {
    MaterialTheme {
        CreatePackScreen(
            state = CreatePackState(
                name = "Funny Cats",
                publisher = "CatLover",
                trayImagePath = "",
                stickers = listOf(DraftSticker(""), DraftSticker("")),
                isEditing = true
            ),
            onIntent = {},
            onBackClick = {},
            onNavigateToCropSticker = {},
            onNavigateToCropTray = {}
        )
    }
}

@Preview
@Composable
private fun CreatePackScreenLoadingPreview() {
    MaterialTheme {
        CreatePackScreen(
            state = CreatePackState(isLoading = true),
            onIntent = {},
            onBackClick = {},
            onNavigateToCropSticker = {},
            onNavigateToCropTray = {}
        )
    }
}
