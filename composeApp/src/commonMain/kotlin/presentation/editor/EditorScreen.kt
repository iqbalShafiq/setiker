package presentation.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.Sticker
import domain.model.TextDecoration
import org.jetbrains.compose.resources.stringResource
import presentation.components.AddTextDecorationBottomSheet
import presentation.components.AiGenerateBottomSheet
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.CheckerboardBackground
import presentation.components.ColorPickerBottomSheet
import presentation.components.DecorationActionChip
import presentation.components.DecorationPreviewLayer
import presentation.components.EditTextDecorationBottomSheet
import presentation.components.EmojiPickerBottomSheet
import presentation.components.FontPickerBottomSheet
import presentation.components.FontWeightPickerBottomSheet
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalAddTagPill
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.SelectableStickerGrid
import presentation.components.StickerEmojiTagChip
import presentation.components.rememberImagePicker
import presentation.createpack.DraftSticker
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.accessibility_text
import setiker.composeapp.generated.resources.accessibility_text_example
import setiker.composeapp.generated.resources.accessibility_text_placeholder
import setiker.composeapp.generated.resources.add
import setiker.composeapp.generated.resources.add_decoration
import setiker.composeapp.generated.resources.add_emoji
import setiker.composeapp.generated.resources.add_image
import setiker.composeapp.generated.resources.add_text
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.change_color
import setiker.composeapp.generated.resources.change_emoji
import setiker.composeapp.generated.resources.change_font
import setiker.composeapp.generated.resources.change_font_weight
import setiker.composeapp.generated.resources.change_image
import setiker.composeapp.generated.resources.crop
import setiker.composeapp.generated.resources.decoration_hint
import setiker.composeapp.generated.resources.edit_sticker_title
import setiker.composeapp.generated.resources.edit_text_decoration
import setiker.composeapp.generated.resources.editor_action_hint
import setiker.composeapp.generated.resources.editor_remove_bg_progress_hint
import setiker.composeapp.generated.resources.generate_ai
import setiker.composeapp.generated.resources.generate_pick_result
import setiker.composeapp.generated.resources.generate_replace_sticker_subtitle
import setiker.composeapp.generated.resources.generate_replace_sticker_title
import setiker.composeapp.generated.resources.remove_background_title
import setiker.composeapp.generated.resources.remove_bg
import setiker.composeapp.generated.resources.remove_bg_preview_content_description
import setiker.composeapp.generated.resources.remove_bg_result_hint
import setiker.composeapp.generated.resources.result_confirmation_title
import setiker.composeapp.generated.resources.save_sticker
import setiker.composeapp.generated.resources.saving
import setiker.composeapp.generated.resources.processing
import setiker.composeapp.generated.resources.select_image
import setiker.composeapp.generated.resources.sticker_preview
import setiker.composeapp.generated.resources.tags_with_count
import setiker.composeapp.generated.resources.text_decoration
import setiker.composeapp.generated.resources.text_decoration_placeholder
import setiker.composeapp.generated.resources.use_result

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val selectedDecoration = state.decorations.firstOrNull { it.id == state.selectedDecorationId }
    var isFontSheetOpen by remember { mutableStateOf(false) }
    var isFontWeightSheetOpen by remember { mutableStateOf(false) }
    var isColorSheetOpen by remember { mutableStateOf(false) }
    var isEditTextSheetOpen by remember { mutableStateOf(false) }

    val decorationImagePicker = rememberImagePicker { path ->
        path?.let {
            onIntent(EditorIntent.AddImageDecorationFromGallery(it))
        }
    }
    val replaceDecorationImagePicker = rememberImagePicker { path ->
        val selected = selectedDecoration
        if (path != null && selected is ImageDecoration) {
            onIntent(EditorIntent.UpdateImageDecorationPath(selected.id, path))
        }
    }
    val generateInputImagePicker = rememberImagePicker { path ->
        path?.let { onIntent(EditorIntent.UpdateGenerateInputImage(it)) }
    }
    var selectedGeneratedIndex by remember(state.generatedPreview) { mutableStateOf<Int?>(null) }
    val isOperationInProgress = state.isSaving || state.isApiLoading || state.isBackgroundRemoving
    val bottomOperationLabel = when {
        state.isSaving -> stringResource(Res.string.saving)
        state.isApiLoading || state.isBackgroundRemoving -> stringResource(Res.string.processing)
        else -> null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.edit_sticker_title),
                onBackClick = null
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                PackBottomBar(
                    actionStatusText = bottomOperationLabel,
                    actions = {
                        PackBottomBarIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            onClick = onBackClick,
                            enabled = !isOperationInProgress
                        )
                        if (selectedDecoration == null) {
                            PackBottomBarIconButton(
                                icon = Icons.Filled.Crop,
                                contentDescription = stringResource(Res.string.crop),
                                onClick = { onIntent(EditorIntent.NavigateToCrop) },
                                enabled = state.imagePath.isNotBlank() && !isOperationInProgress
                            )
                            PackBottomBarIconButton(
                                icon = Icons.Filled.LayersClear,
                                contentDescription = stringResource(Res.string.remove_bg),
                                onClick = { onIntent(EditorIntent.RemoveBackground) },
                                enabled = state.imagePath.isNotBlank() && !isOperationInProgress
                            )
                            PackBottomBarIconButton(
                                icon = Icons.Filled.AutoAwesome,
                                contentDescription = stringResource(Res.string.generate_ai),
                                onClick = { onIntent(EditorIntent.OpenAiGenerateSheet) },
                                enabled = !isOperationInProgress
                            )
                        }
                        when (selectedDecoration) {
                            is TextDecoration -> {
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.Edit,
                                    contentDescription = stringResource(Res.string.edit_text_decoration),
                                    onClick = { isEditTextSheetOpen = true },
                                    enabled = !isOperationInProgress
                                )
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.FontDownload,
                                    contentDescription = stringResource(Res.string.change_font),
                                    onClick = { isFontSheetOpen = true },
                                    enabled = !isOperationInProgress
                                )
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.FormatBold,
                                    contentDescription = stringResource(Res.string.change_font_weight),
                                    onClick = { isFontWeightSheetOpen = true },
                                    enabled = !isOperationInProgress
                                )
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.FormatColorText,
                                    contentDescription = stringResource(Res.string.change_color),
                                    onClick = { isColorSheetOpen = true },
                                    enabled = !isOperationInProgress
                                )
                            }

                            is EmojiDecoration -> {
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.TagFaces,
                                    contentDescription = stringResource(Res.string.change_emoji),
                                    onClick = {
                                        onIntent(EditorIntent.ShowDecorationEmojiPicker(selectedDecoration.id))
                                    },
                                    enabled = !isOperationInProgress
                                )
                            }

                            is ImageDecoration -> {
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.Image,
                                    contentDescription = stringResource(Res.string.change_image),
                                    onClick = { replaceDecorationImagePicker.launch() },
                                    enabled = !isOperationInProgress
                                )
                            }

                            null -> Unit
                        }
                    },
                    floatingActionButton = {
                        PackBottomBarFab(
                            icon = Icons.Filled.Check,
                            contentDescription = stringResource(Res.string.save_sticker),
                            onClick = { onIntent(EditorIntent.SaveSticker) },
                            enabled = !isOperationInProgress,
                            isLoading = state.isSaving
                        )
                    }
                )
            }
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
            val editorScrollState = rememberScrollState()
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                NeubrutalStickerPreviewFrame {
                    if (state.imagePath.isNotBlank()) {
                        key(state.imagePath) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(state.imagePath),
                                    contentDescription = stringResource(Res.string.sticker_preview),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(NeubrutalCardRadius)),
                                    contentScale = ContentScale.Fit
                                )
                                DecorationPreviewLayer(
                                    decorations = state.decorations,
                                    selectedDecorationId = state.selectedDecorationId,
                                    onSelectDecoration = { onIntent(EditorIntent.SelectDecoration(it)) },
                                    onUpdateDecoration = { id, centerX, centerY, scale ->
                                        onIntent(
                                            EditorIntent.UpdateDecorationTransform(
                                                id = id,
                                                centerX = centerX,
                                                centerY = centerY,
                                                scale = scale
                                            )
                                        )
                                    },
                                    onDeleteDecoration = { id ->
                                        onIntent(EditorIntent.RemoveDecoration(id))
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.select_image),
                            style = MaterialTheme.typography.bodyLarge,
                            color = neubrutalSubtleOnSurface()
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(editorScrollState)
                ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.decoration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DecorationActionChip(
                        icon = Icons.Default.TextFields,
                        label = stringResource(Res.string.add_text),
                        onClick = { onIntent(EditorIntent.ShowTextDecorationSheet) }
                    )
                    DecorationActionChip(
                        icon = Icons.Default.TagFaces,
                        label = stringResource(Res.string.add_emoji),
                        onClick = { onIntent(EditorIntent.ShowDecorationEmojiPicker()) }
                    )
                    DecorationActionChip(
                        icon = Icons.Default.Image,
                        label = stringResource(Res.string.add_image),
                        onClick = { decorationImagePicker.launch() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(Res.string.editor_action_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Emoji Tags
                Text(
                    text = stringResource(Res.string.tags_with_count, state.emojis.size, Sticker.MAX_EMOJIS),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = neubrutalOnSurface()
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.emojis.forEachIndexed { index, emoji ->
                        StickerEmojiTagChip(
                            emoji = emoji,
                            onRemove = { onIntent(EditorIntent.RemoveEmoji(index)) }
                        )
                    }

                    if (state.emojis.size < Sticker.MAX_EMOJIS) {
                        NeubrutalAddTagPill(
                            label = stringResource(Res.string.add),
                            onClick = { onIntent(EditorIntent.ShowEmojiPicker) },
                            enabled = !isOperationInProgress
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Accessibility Text
                AppTextField(
                    value = state.accessibilityText,
                    onValueChange = { onIntent(EditorIntent.UpdateAccessibilityText(it)) },
                    label = stringResource(Res.string.accessibility_text),
                    placeholder = stringResource(Res.string.accessibility_text_placeholder),
                    singleLine = false,
                    maxLines = 3,
                    enabled = !isOperationInProgress
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.accessibility_text_example),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )

                Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    if (state.showEmojiPicker) {
        EmojiPickerBottomSheet(
            recentEmojis = state.recentEmojis,
            onEmojiSelected = { emoji ->
                onIntent(EditorIntent.AddEmoji(emoji))
                onIntent(EditorIntent.HideEmojiPicker)
            },
            onDismiss = { onIntent(EditorIntent.HideEmojiPicker) }
        )
    }

    if (state.showDecorationEmojiPicker) {
        EmojiPickerBottomSheet(
            recentEmojis = state.recentEmojis,
            onEmojiSelected = { emoji ->
                val targetId = state.decorationEmojiPickerTargetId
                if (targetId != null) {
                    onIntent(EditorIntent.UpdateEmojiDecorationValue(targetId, emoji))
                } else {
                    onIntent(EditorIntent.AddEmojiDecoration(emoji))
                }
                onIntent(EditorIntent.HideDecorationEmojiPicker)
            },
            onDismiss = { onIntent(EditorIntent.HideDecorationEmojiPicker) }
        )
    }

    if (state.isTextDecorationSheetOpen) {
        AddTextDecorationBottomSheet(
            onAdd = { text, font ->
                onIntent(EditorIntent.AddTextDecoration(text, font))
            },
            onDismiss = { onIntent(EditorIntent.HideTextDecorationSheet) }
        )
    }
    if (isEditTextSheetOpen && selectedDecoration is TextDecoration) {
        EditTextDecorationBottomSheet(
            initialText = selectedDecoration.text,
            onConfirm = { text ->
                onIntent(EditorIntent.UpdateTextDecorationText(selectedDecoration.id, text))
                isEditTextSheetOpen = false
            },
            onDismiss = { isEditTextSheetOpen = false }
        )
    }
    if (isFontSheetOpen && selectedDecoration is TextDecoration) {
        FontPickerBottomSheet(
            selectedFont = selectedDecoration.font,
            onSelect = { font ->
                onIntent(EditorIntent.UpdateTextDecorationFont(selectedDecoration.id, font))
            },
            onDismiss = { isFontSheetOpen = false }
        )
    }
    if (isFontWeightSheetOpen && selectedDecoration is TextDecoration) {
        FontWeightPickerBottomSheet(
            selectedWeight = selectedDecoration.fontWeight,
            onSelect = { weight ->
                onIntent(EditorIntent.UpdateTextDecorationFontWeight(selectedDecoration.id, weight))
            },
            onDismiss = { isFontWeightSheetOpen = false }
        )
    }
    if (isColorSheetOpen && selectedDecoration is TextDecoration) {
        ColorPickerBottomSheet(
            selectedColorArgb = selectedDecoration.textColorArgb,
            onSelect = { color ->
                onIntent(EditorIntent.UpdateTextDecorationColor(selectedDecoration.id, color))
                isColorSheetOpen = false
            },
            onDismiss = { isColorSheetOpen = false }
        )
    }

    if (state.aiGenerateSheetOpen) {
        AiGenerateBottomSheet(
            prompt = state.generatePrompt,
            onPromptChange = { onIntent(EditorIntent.UpdateGeneratePrompt(it)) },
            generateAsGrid = state.generateAsGrid,
            onToggleGrid = { onIntent(EditorIntent.ToggleGenerateAsGrid(it)) },
            gridLayout = state.gridLayout,
            onGridLayoutChange = { onIntent(EditorIntent.UpdateGridLayout(it)) },
            normalizeOutput = state.normalizeOutput,
            onToggleNormalize = { onIntent(EditorIntent.ToggleNormalize(it)) },
            inputImagePath = state.generateInputImage,
            onPickInputImage = { generateInputImagePicker.launch() },
            onClearInputImage = { onIntent(EditorIntent.UpdateGenerateInputImage(null)) },
            // The reference image defaults to the current sticker; emphasise this in copy.
            hasContextualDefault = true,
            isGenerating = state.isApiLoading,
            onGenerate = { onIntent(EditorIntent.GenerateSticker) },
            onDismiss = { onIntent(EditorIntent.CloseAiGenerateSheet) }
        )
    }

    if (state.generatedPreview.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(EditorIntent.CloseGeneratedSheet) },
            sheetState = sheetState,
            containerColor = neubrutalScreenBackground(),
            scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.generate_replace_sticker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = neubrutalOnSurface()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.generate_replace_sticker_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Reuse the same selectable grid component as pack editor, but only allow a
                // single selection — replacement is a single-sticker operation.
                SelectableStickerGrid(
                    stickers = state.generatedPreview.map { DraftSticker(it) },
                    selectedIndices = selectedGeneratedIndex?.let { setOf(it) } ?: emptySet(),
                    onToggle = { index ->
                        selectedGeneratedIndex = if (selectedGeneratedIndex == index) null else index
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppPrimaryButton(
                    text = stringResource(Res.string.generate_pick_result),
                    enabled = selectedGeneratedIndex != null && !isOperationInProgress,
                    onClick = {
                        val idx = selectedGeneratedIndex ?: return@AppPrimaryButton
                        val path = state.generatedPreview.getOrNull(idx) ?: return@AppPrimaryButton
                        onIntent(EditorIntent.ApplyGeneratedImage(path))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(EditorIntent.CloseGeneratedSheet) },
                    enabled = !isOperationInProgress
                )
            }
        }
    }

    if (state.isBackgroundRemoverSheetOpen) {
        val bgRemovalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(EditorIntent.DismissBackgroundRemoverSheet) },
            sheetState = bgRemovalSheetState,
            containerColor = neubrutalScreenBackground(),
            scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                if (state.isBackgroundRemoving) {
                    Text(
                        text = stringResource(Res.string.remove_background_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = neubrutalOnSurface()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.editor_remove_bg_progress_hint),
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
                            .border(NeubrutalBorderWidth, neubrutalBorderColor(), RoundedCornerShape(NeubrutalCardRadius))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                } else {
                    val previewPath = state.backgroundRemoverPreviewPath
                    if (!previewPath.isNullOrBlank()) {
                        Text(
                            text = stringResource(Res.string.result_confirmation_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = neubrutalOnSurface()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.remove_bg_result_hint),
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
                                .border(NeubrutalBorderWidth, neubrutalBorderColor(), RoundedCornerShape(NeubrutalCardRadius))
                                .padding(4.dp)
                        ) {
                            CheckerboardBackground(modifier = Modifier.fillMaxSize())
                            Image(
                                painter = rememberAsyncImagePainter(previewPath),
                                contentDescription = stringResource(Res.string.remove_bg_preview_content_description),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AppPrimaryButton(
                            text = stringResource(Res.string.use_result),
                            onClick = { onIntent(EditorIntent.ConfirmBackgroundRemoval) },
                            enabled = !isOperationInProgress
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppSecondaryButton(
                            text = stringResource(Res.string.cancel),
                            onClick = { onIntent(EditorIntent.DismissBackgroundRemoverSheet) },
                            enabled = !isOperationInProgress
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun EditorScreenPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(
                imagePath = "",
                emojis = listOf("😂", "🐱", "❤️"),
                accessibilityText = "A laughing cat sticker",
                showEmojiPicker = false
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun EditorScreenEmptyPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun EditorScreenLoadingPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(isLoading = true),
            onIntent = {},
            onBackClick = {}
        )
    }
}
