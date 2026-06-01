package presentation.animatededitor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.Sticker
import domain.model.TextDecoration
import org.jetbrains.compose.resources.stringResource
import presentation.common.resolveLocal
import presentation.components.AppIllustration
import presentation.components.AddTextDecorationBottomSheet
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.ColorPickerBottomSheet
import presentation.components.DecorationActionChip
import presentation.components.DecorationPreviewLayer
import presentation.components.EditTextDecorationBottomSheet
import presentation.components.EmojiPickerBottomSheet
import presentation.components.FontPickerBottomSheet
import presentation.components.BorderStyleBottomSheet
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.MediaPreviewBottomBar
import presentation.components.NeubrutalAddTagPill
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.ProgressDialog
import presentation.components.ScreenSectionTitle
import presentation.components.StickerEmojiTagChip
import presentation.theme.NeubrutalCardRadius
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import presentation.components.rememberImagePicker
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.accessibility_text
import setiker.composeapp.generated.resources.accessibility_text_placeholder
import setiker.composeapp.generated.resources.add
import setiker.composeapp.generated.resources.add_decoration
import setiker.composeapp.generated.resources.add_emoji
import setiker.composeapp.generated.resources.add_image
import setiker.composeapp.generated.resources.add_text
import setiker.composeapp.generated.resources.apply_all_frames
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.change_color
import setiker.composeapp.generated.resources.change_border_color
import setiker.composeapp.generated.resources.change_border_thickness
import setiker.composeapp.generated.resources.change_emoji
import setiker.composeapp.generated.resources.change_font
import setiker.composeapp.generated.resources.change_image
import setiker.composeapp.generated.resources.edit_animated_sticker_title
import setiker.composeapp.generated.resources.edit_text_decoration
import setiker.composeapp.generated.resources.encoding_progress_title
import setiker.composeapp.generated.resources.encoding_webp
import setiker.composeapp.generated.resources.frame_index
import setiker.composeapp.generated.resources.frame_preview_cd
import setiker.composeapp.generated.resources.loading_frames
import setiker.composeapp.generated.resources.next_frame
import setiker.composeapp.generated.resources.previous_frame
import setiker.composeapp.generated.resources.save_sticker
import setiker.composeapp.generated.resources.tags_with_count
import setiker.composeapp.generated.resources.this_frame_only
import util.decodeImageBitmap

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimatedEditorScreen(
    state: AnimatedEditorState,
    onIntent: (AnimatedEditorIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val frameBitmap: ImageBitmap? by remember(state.frames, state.currentFrameIndex) {
        derivedStateOf {
            state.frames.getOrNull(state.currentFrameIndex)?.bytes?.let { decodeImageBitmap(it) }
        }
    }

    // Mirror EditorScreen: the bottom bar swaps between playback (media) controls and
    // decoration-specific controls based on the currently selected decoration. Selection is
    // looked up against the visible decorations so per-frame and shared decorations work.
    val selectedDecoration = state.visibleDecorations.firstOrNull { it.id == state.selectedDecorationId }
    var isEditTextSheetOpen by remember { mutableStateOf(false) }
    var isFontSheetOpen by remember { mutableStateOf(false) }
    var isColorSheetOpen by remember { mutableStateOf(false) }
    var isBorderSheetOpen by remember { mutableStateOf(false) }

    val decorationImagePicker = rememberImagePicker { path ->
        path?.let { onIntent(AnimatedEditorIntent.AddImageDecoration(it)) }
    }
    val replaceDecorationImagePicker = rememberImagePicker { path ->
        val selected = selectedDecoration
        if (path != null && selected is ImageDecoration) {
            onIntent(AnimatedEditorIntent.UpdateImageDecorationPath(selected.id, path))
        }
    }

    val isReadyToSave = state.frames.isNotEmpty() && !state.isSaving
    val bottomOperationLabel = if (state.isSaving) {
        state.saveProgressLabel ?: state.backgroundJobMessage ?: stringResource(Res.string.encoding_webp)
    } else {
        null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Back button removed: navigation lives in the bottom bar (cancel icon).
            AppTopBar(
                title = stringResource(Res.string.edit_animated_sticker_title),
                onBackClick = null
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                if (selectedDecoration == null) {
                    // Default mode: playback controls + frame scrubber + Save.
                    MediaPreviewBottomBar(
                        primaryIcon = Icons.Filled.Check,
                        primaryDescription = stringResource(Res.string.save_sticker),
                        onPrimary = { onIntent(AnimatedEditorIntent.Save) },
                        primaryEnabled = isReadyToSave,
                        primaryLoading = state.isSaving,
                        onCancel = onBackClick,
                        cancelEnabled = !state.isSaving,
                        actionStatusText = bottomOperationLabel,
                        isPlaying = state.isPlaying,
                        playEnabled = state.frames.size > 1 && !state.isSaving,
                        onTogglePlay = {
                            if (state.isPlaying) onIntent(AnimatedEditorIntent.PausePreview)
                            else onIntent(AnimatedEditorIntent.PlayPreview)
                        },
                        extraActions = {
                            PackBottomBarIconButton(
                                icon = Icons.Filled.SkipPrevious,
                                contentDescription = stringResource(Res.string.previous_frame),
                                onClick = { onIntent(AnimatedEditorIntent.ScrubToFrame(state.currentFrameIndex - 1)) },
                                enabled = state.currentFrameIndex > 0 && !state.isPlaying && !state.isSaving
                            )
                            PackBottomBarIconButton(
                                icon = Icons.Filled.SkipNext,
                                contentDescription = stringResource(Res.string.next_frame),
                                onClick = { onIntent(AnimatedEditorIntent.ScrubToFrame(state.currentFrameIndex + 1)) },
                                enabled = state.currentFrameIndex < state.frames.size - 1 && !state.isPlaying && !state.isSaving
                            )
                        }
                    )
                } else {
                    // Decoration-focused mode: hide playback controls and surface the same
                    // decoration-specific buttons used by the static editor for parity.
                    // "Back" deselects the decoration so the user can return to the playback bar.
                    PackBottomBar(
                        actionStatusText = bottomOperationLabel,
                        actions = {
                            PackBottomBarIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back),
                                onClick = { onIntent(AnimatedEditorIntent.SelectDecoration(null)) },
                                enabled = !state.isSaving
                            )
                            when (selectedDecoration) {
                                is TextDecoration -> {
                                    PackBottomBarIconButton(
                                        icon = Icons.Filled.Edit,
                                        contentDescription = stringResource(Res.string.edit_text_decoration),
                                        onClick = { isEditTextSheetOpen = true },
                                        enabled = !state.isSaving
                                    )
                                    PackBottomBarIconButton(
                                        icon = Icons.Filled.FontDownload,
                                        contentDescription = stringResource(Res.string.change_font),
                                        onClick = { isFontSheetOpen = true },
                                        enabled = !state.isSaving
                                    )
                                    PackBottomBarIconButton(
                                        icon = Icons.Filled.FormatColorText,
                                        contentDescription = stringResource(Res.string.change_color),
                                        onClick = { isColorSheetOpen = true },
                                        enabled = !state.isSaving
                                    )
                                    PackBottomBarIconButton(
                                        icon = Icons.Filled.BorderColor,
                                        contentDescription = stringResource(Res.string.change_border_thickness),
                                        onClick = { isBorderSheetOpen = true },
                                        enabled = !state.isSaving
                                    )
                                }
                                is EmojiDecoration -> {
                                    PackBottomBarIconButton(
                                        icon = Icons.Filled.TagFaces,
                                        contentDescription = stringResource(Res.string.change_emoji),
                                        onClick = {
                                            onIntent(
                                                AnimatedEditorIntent.ShowDecorationEmojiPicker(
                                                    selectedDecoration.id
                                                )
                                            )
                                        },
                                        enabled = !state.isSaving
                                    )
                                    PackBottomBarIconButton(
                                        icon = Icons.Filled.BorderColor,
                                        contentDescription = stringResource(Res.string.change_border_thickness),
                                        onClick = { isBorderSheetOpen = true },
                                        enabled = !state.isSaving
                                    )
                                }
                                is ImageDecoration -> {
                                    PackBottomBarIconButton(
                                        icon = Icons.Filled.Image,
                                        contentDescription = stringResource(Res.string.change_image),
                                        onClick = { replaceDecorationImagePicker.launch() },
                                        enabled = !state.isSaving
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            PackBottomBarFab(
                                icon = Icons.Filled.Check,
                                contentDescription = stringResource(Res.string.save_sticker),
                                onClick = { onIntent(AnimatedEditorIntent.Save) },
                                enabled = isReadyToSave,
                                isLoading = state.isSaving
                            )
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingIndicator(
                modifier = modifier.fillMaxSize().padding(innerPadding),
                illustration = AppIllustration.VideoTools
            )
        } else {
            InteractionBlockedBox(
                blocked = state.isSaving,
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                NeubrutalStickerPreviewFrame {
                    Box(modifier = Modifier.fillMaxSize()) {
                        frameBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = stringResource(
                                    Res.string.frame_preview_cd,
                                    state.currentFrameIndex + 1
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(NeubrutalCardRadius)),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text(
                            text = stringResource(Res.string.loading_frames),
                            color = neubrutalSubtleOnSurface(),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        DecorationPreviewLayer(
                            decorations = state.visibleDecorations,
                            selectedDecorationId = state.selectedDecorationId,
                            onSelectDecoration = { onIntent(AnimatedEditorIntent.SelectDecoration(it)) },
                            onUpdateDecoration = { id, cx, cy, sc ->
                                onIntent(
                                    AnimatedEditorIntent.UpdateDecorationTransform(
                                        id = id,
                                        centerX = cx,
                                        centerY = cy,
                                        scale = sc
                                    )
                                )
                            },
                            onDeleteDecoration = { id ->
                                onIntent(AnimatedEditorIntent.RemoveDecoration(id))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.frames.size > 1) {
                    Text(
                        text = stringResource(
                            Res.string.frame_index,
                            state.currentFrameIndex + 1,
                            state.totalFrames
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface()
                    )
                    Slider(
                        value = state.currentFrameIndex.toFloat(),
                        onValueChange = { onIntent(AnimatedEditorIntent.ScrubToFrame(it.toInt())) },
                        valueRange = 0f..(state.frames.size - 1).coerceAtLeast(1).toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.applyScope == DecorationApplyScope.AllFrames,
                            onClick = { onIntent(AnimatedEditorIntent.SetApplyScope(DecorationApplyScope.AllFrames)) },
                            label = { Text(stringResource(Res.string.apply_all_frames)) }
                        )
                        FilterChip(
                            selected = state.applyScope == DecorationApplyScope.CurrentFrameOnly,
                            onClick = {
                                onIntent(
                                    AnimatedEditorIntent.SetApplyScope(DecorationApplyScope.CurrentFrameOnly)
                                )
                            },
                            label = { Text(stringResource(Res.string.this_frame_only)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ScreenSectionTitle(text = stringResource(Res.string.add_decoration))
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DecorationActionChip(
                            icon = Icons.Default.TextFields,
                            label = stringResource(Res.string.add_text),
                            onClick = { onIntent(AnimatedEditorIntent.ShowTextDecorationSheet) }
                        )
                        DecorationActionChip(
                            icon = Icons.Default.TagFaces,
                            label = stringResource(Res.string.add_emoji),
                            onClick = { onIntent(AnimatedEditorIntent.ShowDecorationEmojiPicker()) }
                        )
                        DecorationActionChip(
                            icon = Icons.Default.Image,
                            label = stringResource(Res.string.add_image),
                            onClick = { decorationImagePicker.launch() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ScreenSectionTitle(
                        text = stringResource(
                            Res.string.tags_with_count,
                            state.emojis.size,
                            Sticker.MAX_EMOJIS
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.emojis.forEachIndexed { index, emoji ->
                            StickerEmojiTagChip(
                                emoji = emoji,
                                onRemove = { onIntent(AnimatedEditorIntent.RemoveEmojiTag(index)) }
                            )
                        }
                        if (state.emojis.size < Sticker.MAX_EMOJIS) {
                            NeubrutalAddTagPill(
                                label = stringResource(Res.string.add),
                                onClick = { onIntent(AnimatedEditorIntent.ShowEmojiPicker) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    AppTextField(
                        value = state.accessibilityText,
                        onValueChange = { onIntent(AnimatedEditorIntent.UpdateAccessibilityText(it)) },
                        label = stringResource(Res.string.accessibility_text),
                        placeholder = stringResource(Res.string.accessibility_text_placeholder),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    state.error?.let { error ->
                        Text(
                            text = error.resolveLocal(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }
                }
            }
        }
    }

    if (state.showEmojiPicker) {
        EmojiPickerBottomSheet(
            recentEmojis = state.recentEmojis,
            onEmojiSelected = { emoji ->
                onIntent(AnimatedEditorIntent.AddEmojiTag(emoji))
                onIntent(AnimatedEditorIntent.HideEmojiPicker)
            },
            onDismiss = { onIntent(AnimatedEditorIntent.HideEmojiPicker) }
        )
    }
    if (state.showDecorationEmojiPicker) {
        EmojiPickerBottomSheet(
            recentEmojis = state.recentEmojis,
            onEmojiSelected = { emoji ->
                val target = state.decorationEmojiPickerTargetId
                if (target != null) {
                    onIntent(AnimatedEditorIntent.UpdateEmojiDecoration(target, emoji))
                } else {
                    onIntent(AnimatedEditorIntent.AddEmojiDecoration(emoji))
                }
                onIntent(AnimatedEditorIntent.HideDecorationEmojiPicker)
            },
            onDismiss = { onIntent(AnimatedEditorIntent.HideDecorationEmojiPicker) }
        )
    }
    if (state.showTextDecorationSheet) {
        AddTextDecorationBottomSheet(
            onAdd = { text, font ->
                onIntent(AnimatedEditorIntent.AddTextDecoration(text, font))
            },
            onDismiss = { onIntent(AnimatedEditorIntent.HideTextDecorationSheet) }
        )
    }

    // Decoration-specific sheets — same set as EditorScreen so behaviour matches the static editor.
    if (isEditTextSheetOpen && selectedDecoration is TextDecoration) {
        EditTextDecorationBottomSheet(
            initialText = selectedDecoration.text,
            onConfirm = { text ->
                onIntent(AnimatedEditorIntent.UpdateTextDecorationText(selectedDecoration.id, text))
                isEditTextSheetOpen = false
            },
            onDismiss = { isEditTextSheetOpen = false }
        )
    }
    if (isFontSheetOpen && selectedDecoration is TextDecoration) {
        FontPickerBottomSheet(
            selectedFont = selectedDecoration.font,
            selectedWeight = selectedDecoration.fontWeight,
            onSelectFont = { font ->
                onIntent(AnimatedEditorIntent.UpdateTextDecorationFont(selectedDecoration.id, font))
            },
            onSelectWeight = { weight ->
                onIntent(AnimatedEditorIntent.UpdateTextDecorationFontWeight(selectedDecoration.id, weight))
            },
            onDismiss = { isFontSheetOpen = false }
        )
    }
    if (isColorSheetOpen && selectedDecoration is TextDecoration) {
        ColorPickerBottomSheet(
            selectedColorArgb = selectedDecoration.textColorArgb,
            onSelect = { color ->
                onIntent(AnimatedEditorIntent.UpdateTextDecorationColor(selectedDecoration.id, color))
                isColorSheetOpen = false
            },
            onDismiss = { isColorSheetOpen = false }
        )
    }
    if (isBorderSheetOpen && selectedDecoration is TextDecoration) {
        BorderStyleBottomSheet(
            initialBorderColorArgb = selectedDecoration.borderColorArgb,
            initialWidthRatio = selectedDecoration.borderWidthRatio,
            onSelect = { color, width ->
                onIntent(AnimatedEditorIntent.UpdateTextDecorationBorderColor(selectedDecoration.id, color))
                onIntent(AnimatedEditorIntent.UpdateTextDecorationBorderWidth(selectedDecoration.id, width))
                isBorderSheetOpen = false
            },
            onDismiss = { isBorderSheetOpen = false }
        )
    }
    if (isBorderSheetOpen && selectedDecoration is EmojiDecoration) {
        BorderStyleBottomSheet(
            initialBorderColorArgb = selectedDecoration.borderColorArgb,
            initialWidthRatio = selectedDecoration.borderWidthRatio,
            onSelect = { color, width ->
                onIntent(AnimatedEditorIntent.UpdateEmojiDecorationBorderColor(selectedDecoration.id, color))
                onIntent(AnimatedEditorIntent.UpdateEmojiDecorationBorderWidth(selectedDecoration.id, width))
                isBorderSheetOpen = false
            },
            onDismiss = { isBorderSheetOpen = false }
        )
    }

    if (state.isSaving) {
        // Real progress: forwarded from saveAnimatedStickerImage's onProgress callback.
        val label = state.saveProgressLabel ?: state.backgroundJobMessage ?: stringResource(Res.string.encoding_webp)
        ProgressDialog(
            title = stringResource(Res.string.encoding_progress_title),
            progress = state.saveProgress,
            progressLabel = label
        )
    }
}

// AnimatedEditorTextSheet has been replaced by the shared `AddTextDecorationBottomSheet`
// component so both static and animated editors use the same UI (including font picker)
// when adding a text decoration.

// MARK: - Previews

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview
@Composable
private fun AnimatedEditorScreenPreview() {
    MaterialTheme {
        AnimatedEditorScreen(
            state = AnimatedEditorState(
                draftId = "preview",
                frames = listOf(),
                currentFrameIndex = 0,
                emojis = listOf("😂", "🐱"),
                accessibilityText = "A funny cat sticker"
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview
@Composable
private fun AnimatedEditorScreenLoadingPreview() {
    MaterialTheme {
        AnimatedEditorScreen(
            state = AnimatedEditorState(isLoading = true),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview
@Composable
private fun AnimatedEditorScreenSavingPreview() {
    MaterialTheme {
        AnimatedEditorScreen(
            state = AnimatedEditorState(
                draftId = "preview",
                frames = listOf(),
                isSaving = true,
                saveProgress = 0.6f,
                saveProgressLabel = "Encoding frame 12/20"
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}
