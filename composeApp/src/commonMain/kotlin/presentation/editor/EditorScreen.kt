package presentation.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.Sticker
import domain.model.TextDecoration
import presentation.components.CheckerboardBackground
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.EmojiPickerBottomSheet
import presentation.components.DecorationPreviewLayer
import presentation.components.LoadingIndicator
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.rememberImagePicker
import presentation.theme.AccentCoral
import presentation.theme.AccentCoralLight
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.accessibility_text
import setiker.composeapp.generated.resources.accessibility_text_example
import setiker.composeapp.generated.resources.accessibility_text_placeholder
import setiker.composeapp.generated.resources.add
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.crop
import setiker.composeapp.generated.resources.edit_sticker_title
import setiker.composeapp.generated.resources.editor_action_hint
import setiker.composeapp.generated.resources.editor_remove_bg_progress_hint
import setiker.composeapp.generated.resources.remove_background_title
import setiker.composeapp.generated.resources.remove_bg
import setiker.composeapp.generated.resources.remove_bg_preview_content_description
import setiker.composeapp.generated.resources.remove_bg_result_hint
import setiker.composeapp.generated.resources.remove_emoji
import setiker.composeapp.generated.resources.result_confirmation_title
import setiker.composeapp.generated.resources.save_sticker
import setiker.composeapp.generated.resources.select_image
import setiker.composeapp.generated.resources.sticker_preview
import setiker.composeapp.generated.resources.use_result
import setiker.composeapp.generated.resources.tags_with_count
import setiker.composeapp.generated.resources.add_text
import setiker.composeapp.generated.resources.add_emoji
import setiker.composeapp.generated.resources.add_image
import setiker.composeapp.generated.resources.decoration_hint
import setiker.composeapp.generated.resources.text_decoration
import setiker.composeapp.generated.resources.text_decoration_placeholder
import setiker.composeapp.generated.resources.add_decoration
import setiker.composeapp.generated.resources.change_font
import setiker.composeapp.generated.resources.change_font_weight
import setiker.composeapp.generated.resources.change_color
import setiker.composeapp.generated.resources.edit_text_decoration
import setiker.composeapp.generated.resources.change_emoji
import setiker.composeapp.generated.resources.change_image

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
                    actions = {
                        PackBottomBarIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            onClick = onBackClick
                        )
                        if (selectedDecoration == null) {
                            PackBottomBarIconButton(
                                icon = Icons.Filled.Crop,
                                contentDescription = stringResource(Res.string.crop),
                                onClick = { onIntent(EditorIntent.NavigateToCrop) },
                                enabled = state.imagePath.isNotBlank()
                            )
                            PackBottomBarIconButton(
                                icon = Icons.Filled.LayersClear,
                                contentDescription = stringResource(Res.string.remove_bg),
                                onClick = { onIntent(EditorIntent.RemoveBackground) },
                                enabled = state.imagePath.isNotBlank() && !state.isBackgroundRemoving
                            )
                        }
                        when (selectedDecoration) {
                            is TextDecoration -> {
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.Edit,
                                    contentDescription = stringResource(Res.string.edit_text_decoration),
                                    onClick = { isEditTextSheetOpen = true }
                                )
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.FontDownload,
                                    contentDescription = stringResource(Res.string.change_font),
                                    onClick = { isFontSheetOpen = true }
                                )
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.FormatBold,
                                    contentDescription = stringResource(Res.string.change_font_weight),
                                    onClick = { isFontWeightSheetOpen = true }
                                )
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.FormatColorText,
                                    contentDescription = stringResource(Res.string.change_color),
                                    onClick = { isColorSheetOpen = true }
                                )
                            }

                            is EmojiDecoration -> {
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.TagFaces,
                                    contentDescription = stringResource(Res.string.change_emoji),
                                    onClick = {
                                        onIntent(EditorIntent.ShowDecorationEmojiPicker(selectedDecoration.id))
                                    }
                                )
                            }

                            is ImageDecoration -> {
                                PackBottomBarIconButton(
                                    icon = Icons.Filled.Image,
                                    contentDescription = stringResource(Res.string.change_image),
                                    onClick = { replaceDecorationImagePicker.launch() }
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
                            enabled = !state.isLoading
                        )
                    }
                )
            }
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
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Preview (Neubrutal Frame)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .neubrutalShadow(
                            offsetX = 4.dp,
                            offsetY = 4.dp,
                            cornerRadius = 20.dp,
                            color = NeubrutalBlack
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeubrutalWhite)
                        .border(
                            width = 2.dp,
                            color = NeubrutalBlack,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imagePath.isNotBlank()) {
                        android.util.Log.d("EditorScreen", "Loading image: ${state.imagePath}")
                        key(state.imagePath) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(state.imagePath),
                                    contentDescription = stringResource(Res.string.sticker_preview),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
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
                            color = NeubrutalGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.decoration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalGray
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
                    color = NeubrutalGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Emoji Tags
                Text(
                    text = stringResource(Res.string.tags_with_count, state.emojis.size, Sticker.MAX_EMOJIS),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeubrutalBlack
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.emojis.forEachIndexed { index, emoji ->
                        EmojiChip(
                            emoji = emoji,
                            onRemove = { onIntent(EditorIntent.RemoveEmoji(index)) }
                        )
                    }

                    if (state.emojis.size < Sticker.MAX_EMOJIS) {
                        NeubrutalOutlinedPill(
                            onClick = { onIntent(EditorIntent.ShowEmojiPicker) },
                            label = stringResource(Res.string.add)
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
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.accessibility_text_example),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalGray
                )

                Spacer(modifier = Modifier.height(120.dp))
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
        TextDecorationBottomSheet(
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

    if (state.isBackgroundRemoverSheetOpen) {
        val bgRemovalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(EditorIntent.DismissBackgroundRemoverSheet) },
            sheetState = bgRemovalSheetState,
            containerColor = NeubrutalBg,
            scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
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
                        color = NeubrutalBlack
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.editor_remove_bg_progress_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeubrutalBlack.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NeubrutalWhite)
                            .border(2.dp, NeubrutalBlack, RoundedCornerShape(16.dp))
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
                            color = NeubrutalBlack
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.remove_bg_result_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = NeubrutalBlack.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(NeubrutalWhite)
                                .border(2.dp, NeubrutalBlack, RoundedCornerShape(16.dp))
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
                            onClick = { onIntent(EditorIntent.ConfirmBackgroundRemoval) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppSecondaryButton(
                            text = stringResource(Res.string.cancel),
                            onClick = { onIntent(EditorIntent.DismissBackgroundRemoverSheet) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTextDecorationBottomSheet(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeubrutalBg,
        scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp)
        ) {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(Res.string.edit_text_decoration),
                placeholder = stringResource(Res.string.text_decoration_placeholder)
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.add_decoration),
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontPickerBottomSheet(
    selectedFont: DecorationFont,
    onSelect: (DecorationFont) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeubrutalBg,
        scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Sample Aa Bb 123",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = mapFontFamily(selectedFont),
                color = NeubrutalBlack
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecorationFont.entries.forEach { font ->
                    FilterChip(
                        selected = selectedFont == font,
                        onClick = { onSelect(font) },
                        label = { Text(font.name, fontFamily = mapFontFamily(font)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontWeightPickerBottomSheet(
    selectedWeight: DecorationFontWeight,
    onSelect: (DecorationFontWeight) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeubrutalBg,
        scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Sample Aa Bb 123",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = mapFontWeight(selectedWeight),
                color = NeubrutalBlack
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecorationFontWeight.entries.forEach { weight ->
                    FilterChip(
                        selected = selectedWeight == weight,
                        onClick = { onSelect(weight) },
                        label = { Text(weight.name, fontWeight = mapFontWeight(weight)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerBottomSheet(
    selectedColorArgb: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorOptions = listOf(
        0xFFFFFFFFL,
        0xFF000000L,
        0xFFFFEB3BL,
        0xFFFF5252L,
        0xFF4CAF50L,
        0xFF2196F3L
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeubrutalBg,
        scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colorOptions.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color.toInt()))
                        .border(
                            if (selectedColorArgb == color) 3.dp else 2.dp,
                            NeubrutalBlack,
                            CircleShape
                        )
                        .clickable { onSelect(color) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextDecorationBottomSheet(
    onAdd: (String, DecorationFont) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedFont by remember { mutableStateOf(DecorationFont.Sans) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeubrutalBg,
        scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(Res.string.text_decoration),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NeubrutalBlack
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(Res.string.add_text),
                placeholder = stringResource(Res.string.text_decoration_placeholder)
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecorationFont.entries.forEach { font ->
                    FilterChip(
                        selected = selectedFont == font,
                        onClick = { selectedFont = font },
                        label = {
                            Text(
                                text = font.name,
                                fontFamily = mapFontFamily(font)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.add_decoration),
                enabled = text.isNotBlank(),
                onClick = { onAdd(text, selectedFont) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppSecondaryButton(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss
            )
        }
    }
}

private fun mapFontFamily(font: DecorationFont): FontFamily = when (font) {
    DecorationFont.Sans -> FontFamily.SansSerif
    DecorationFont.Serif -> FontFamily.Serif
    DecorationFont.Mono -> FontFamily.Monospace
    DecorationFont.Cursive -> FontFamily.Cursive
    DecorationFont.Display -> FontFamily.Serif
    DecorationFont.Rounded -> FontFamily.SansSerif
    DecorationFont.Condensed -> FontFamily.SansSerif
}

private fun mapFontWeight(weight: DecorationFontWeight): FontWeight = when (weight) {
    DecorationFontWeight.Light -> FontWeight.Light
    DecorationFontWeight.Regular -> FontWeight.Normal
    DecorationFontWeight.Medium -> FontWeight.Medium
    DecorationFontWeight.SemiBold -> FontWeight.SemiBold
    DecorationFontWeight.Bold -> FontWeight.Bold
}

@Composable
private fun DecorationActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(NeubrutalWhite)
            .border(2.dp, NeubrutalBlack, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = AccentCoral)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = NeubrutalBlack
        )
    }
}

@Composable
private fun EmojiChip(
    emoji: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AccentCoralLight)
            .border(
                width = 2.dp,
                color = NeubrutalBlack,
                shape = CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(18.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.remove_emoji),
                modifier = Modifier.size(12.dp),
                tint = NeubrutalGray
            )
        }
    }
}

@Composable
private fun NeubrutalOutlinedPill(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(NeubrutalWhite)
            .border(
                width = 2.dp,
                color = NeubrutalBlack,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AccentCoral,
            fontWeight = FontWeight.Medium
        )
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
