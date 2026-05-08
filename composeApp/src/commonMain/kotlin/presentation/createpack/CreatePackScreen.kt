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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.components.SelectableStickerGrid
import presentation.components.rememberImagePicker
import presentation.theme.AccentCoral
import presentation.theme.AccentCoralLight
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.add_sticker
import setiker.composeapp.generated.resources.add_to_pack
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.close
import setiker.composeapp.generated.resources.create_pack_title
import setiker.composeapp.generated.resources.edit_pack_title
import setiker.composeapp.generated.resources.generate
import setiker.composeapp.generated.resources.generate_ai
import setiker.composeapp.generated.resources.generate_ai_sheet_title
import setiker.composeapp.generated.resources.generate_confirm_subtitle
import setiker.composeapp.generated.resources.generate_confirm_title
import setiker.composeapp.generated.resources.generate_grid_hint
import setiker.composeapp.generated.resources.generate_single_hint
import setiker.composeapp.generated.resources.generate_tip
import setiker.composeapp.generated.resources.generating
import setiker.composeapp.generated.resources.grid_confirm_hint
import setiker.composeapp.generated.resources.grid_confirm_title
import setiker.composeapp.generated.resources.grid_off
import setiker.composeapp.generated.resources.grid_on
import setiker.composeapp.generated.resources.grid_source_content_description
import setiker.composeapp.generated.resources.grid_split
import setiker.composeapp.generated.resources.normalize_off
import setiker.composeapp.generated.resources.normalize_on
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder
import setiker.composeapp.generated.resources.processing
import setiker.composeapp.generated.resources.prompt_label
import setiker.composeapp.generated.resources.prompt_placeholder
import setiker.composeapp.generated.resources.publisher_label
import setiker.composeapp.generated.resources.publisher_placeholder
import setiker.composeapp.generated.resources.remove_sticker
import setiker.composeapp.generated.resources.save_pack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePackScreen(
    state: CreatePackState,
    onIntent: (CreatePackIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val trayIconPicker = rememberImagePicker { path ->
        path?.let { onIntent(CreatePackIntent.UpdateTrayImage(it)) }
    }

    val stickerPicker = rememberImagePicker { path ->
        path?.let { onIntent(CreatePackIntent.AddSticker(it)) }
    }

    val gridSourcePicker = rememberImagePicker { path ->
        path?.let {
            onIntent(CreatePackIntent.UpdateGridSplitSource(it))
            onIntent(CreatePackIntent.OpenGridConfirmSheet)
        }
    }

    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val generatedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gridSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (state.aiGenerateSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(CreatePackIntent.CloseAiGenerateSheet) },
            sheetState = aiSheetState,
            containerColor = NeubrutalBg,
            scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.generate_ai_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeubrutalBlack
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppTextField(
                    value = state.generatePrompt,
                    onValueChange = { onIntent(CreatePackIntent.UpdateGeneratePrompt(it)) },
                    label = stringResource(Res.string.prompt_label),
                    placeholder = stringResource(Res.string.prompt_placeholder)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.generate_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalBlack.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.generateAsGrid,
                        onClick = { onIntent(CreatePackIntent.ToggleGenerateAsGrid(true)) },
                        label = { Text(stringResource(Res.string.grid_on)) }
                    )
                    FilterChip(
                        selected = !state.generateAsGrid,
                        onClick = { onIntent(CreatePackIntent.ToggleGenerateAsGrid(false)) },
                        label = { Text(stringResource(Res.string.grid_off)) }
                    )
                }
                if (state.generateAsGrid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("2x2", "3x3", "4x4").forEach { layout ->
                            FilterChip(
                                selected = state.gridLayout == layout,
                                onClick = { onIntent(CreatePackIntent.UpdateGridLayout(layout)) },
                                label = { Text(layout) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilterChip(
                        selected = state.normalizeOutput,
                        onClick = { onIntent(CreatePackIntent.ToggleNormalize(!state.normalizeOutput)) },
                        label = {
                            Text(
                                stringResource(
                                    if (state.normalizeOutput) Res.string.normalize_on else Res.string.normalize_off
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.generate_grid_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeubrutalBlack.copy(alpha = 0.7f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.generate_single_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeubrutalBlack.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                AppPrimaryButton(
                    text = stringResource(if (state.isApiLoading) Res.string.generating else Res.string.generate),
                    enabled = !state.isApiLoading,
                    onClick = { onIntent(CreatePackIntent.GenerateStickers) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.close),
                    onClick = { onIntent(CreatePackIntent.CloseAiGenerateSheet) }
                )
            }
        }
    }

    if (state.generatedPreview.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(CreatePackIntent.CloseGeneratedSheet) },
            sheetState = generatedSheetState,
            containerColor = NeubrutalBg,
            scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.generate_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeubrutalBlack
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.generate_confirm_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalBlack.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        Res.string.selected_count,
                        state.selectedGeneratedPreview.size,
                        state.generatedPreview.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeubrutalBlack.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectableStickerGrid(
                    stickers = state.generatedPreview,
                    selectedIndices = state.selectedGeneratedPreview,
                    onToggle = { onIntent(CreatePackIntent.ToggleGeneratedSelection(it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppPrimaryButton(
                    text = stringResource(Res.string.add_to_pack),
                    enabled = state.selectedGeneratedPreview.isNotEmpty(),
                    onClick = { onIntent(CreatePackIntent.AddSelectedGeneratedToPack) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(CreatePackIntent.CloseGeneratedSheet) }
                )
            }
        }
    }

    if (state.gridSplitSheetPhase != GridSplitSheetPhase.Hidden) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(CreatePackIntent.CloseGridSheet) },
            sheetState = gridSheetState,
            containerColor = NeubrutalBg,
            scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.gridSplitSheetPhase) {
                    GridSplitSheetPhase.ConfirmPick -> {
                        Text(
                            text = stringResource(Res.string.grid_confirm_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeubrutalBlack
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.gridSplitSourcePath.isNotBlank()) {
                            AsyncImage(
                                model = state.gridSplitSourcePath,
                                contentDescription = stringResource(Res.string.grid_source_content_description),
                                modifier = Modifier
                                    .size(160.dp)
                                    .neubrutalShadow(3.dp, 3.dp, 16.dp, NeubrutalBlack)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NeubrutalWhite)
                                    .border(2.dp, NeubrutalBlack, RoundedCornerShape(16.dp))
                                    .padding(4.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.grid_confirm_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeubrutalBlack
                        )
                        if (!state.error.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
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
                            onClick = { onIntent(CreatePackIntent.CloseGridSheet) }
                        )
                    }

                    GridSplitSheetPhase.Results -> {
                        Text(
                            text = stringResource(Res.string.split_result_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeubrutalBlack
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                Res.string.selected_count,
                                state.selectedSplitPreview.size,
                                state.splitPreview.size
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = NeubrutalBlack.copy(alpha = 0.75f)
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
                            onClick = { onIntent(CreatePackIntent.CloseGridSheet) }
                        )
                    }

                }
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
            BottomAppBar(
                containerColor = NeubrutalWhite,
                contentColor = NeubrutalBlack,
                tonalElevation = 0.dp,
                actions = {
                    IconButton(onClick = onBackClick, enabled = !state.isApiLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                    IconButton(
                        onClick = { onIntent(CreatePackIntent.OpenAiGenerateSheet) },
                        enabled = !state.isApiLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(Res.string.generate_ai)
                        )
                    }
                    IconButton(
                        onClick = { gridSourcePicker.launch() },
                        enabled = !state.isApiLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ViewModule,
                            contentDescription = stringResource(Res.string.grid_split)
                        )
                    }
                },
                floatingActionButton = {
                    val fabShape = RoundedCornerShape(16.dp)
                    FloatingActionButton(
                        onClick = { onIntent(CreatePackIntent.SavePack) },
                        containerColor = AccentCoral,
                        contentColor = NeubrutalWhite,
                        shape = fabShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                        modifier = Modifier.border(2.dp, NeubrutalBlack, fabShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(if (state.isEditing) Res.string.update_pack else Res.string.save_pack)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeubrutalBg
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingIndicator(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AppTextField(
                    value = state.name,
                    onValueChange = { onIntent(CreatePackIntent.UpdateName(it)) },
                    label = stringResource(Res.string.pack_name_label),
                    placeholder = stringResource(Res.string.pack_name_placeholder)
                )

                Spacer(modifier = Modifier.height(20.dp))

                AppTextField(
                    value = state.publisher,
                    onValueChange = { onIntent(CreatePackIntent.UpdatePublisher(it)) },
                    label = stringResource(Res.string.publisher_label),
                    placeholder = stringResource(Res.string.publisher_placeholder)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.tray_icon),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeubrutalBlack
                )

                Spacer(modifier = Modifier.height(10.dp))

                TrayIconSelector(
                    imagePath = state.trayImagePath,
                    onClick = { trayIconPicker.launch() }
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
                        color = NeubrutalBlack
                    )

                    IconButton(onClick = { stickerPicker.launch() }) {
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
                    color = NeubrutalBlack.copy(alpha = 0.7f)
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
                            rowItems.forEachIndexed { itemIndex, stickerPath ->
                                val stickerIndex = (rowIndex * 4) + itemIndex
                                StickerPreviewItem(
                                    imagePath = stickerPath,
                                    onRemove = { onIntent(CreatePackIntent.RemoveSticker(stickerIndex)) },
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

@Composable
private fun TrayIconSelector(
    imagePath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (imagePath.isNotBlank()) {
        Box(
            modifier = modifier
                .size(96.dp)
                .neubrutalShadow(
                    offsetX = 3.dp,
                    offsetY = 3.dp,
                    cornerRadius = 16.dp,
                    color = NeubrutalBlack
                )
                .clip(RoundedCornerShape(16.dp))
                .background(NeubrutalWhite)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick),
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
                    offsetX = 3.dp,
                    offsetY = 3.dp,
                    cornerRadius = 16.dp,
                    color = NeubrutalBlack
                )
                .clip(RoundedCornerShape(16.dp))
                .background(AccentCoralLight)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.select_tray_icon),
                modifier = Modifier.size(32.dp),
                tint = NeubrutalBlack
            )
        }
    }
}

@Composable
private fun StickerPreviewItem(
    imagePath: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = stringResource(Res.string.sticker_preview_content_description),
            modifier = Modifier
                .fillMaxSize()
                .neubrutalShadow(
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    cornerRadius = 12.dp,
                    color = NeubrutalBlack
                )
                .clip(RoundedCornerShape(12.dp))
                .background(NeubrutalWhite)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(2.dp),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ErrorRed)
                .border(
                    width = 1.5.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.remove_sticker),
                modifier = Modifier.size(14.dp),
                tint = NeubrutalWhite
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
                stickers = listOf("", "", ""),
                isEditing = false
            ),
            onIntent = {},
            onBackClick = {}
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
                stickers = listOf("", ""),
                isEditing = true
            ),
            onIntent = {},
            onBackClick = {}
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
            onBackClick = {}
        )
    }
}
