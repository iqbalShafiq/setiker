package presentation.videostickerpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import presentation.components.keyboardAwareScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.remote.readFileBytes
import domain.model.ResolvedVideoAnimatedSticker
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.Sticker
import domain.model.AiQuotaOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppIllustration
import presentation.components.AppIllustrationImage
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.AiQuotaSummary
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.StickerCard
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.loading_frames
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder
import setiker.composeapp.generated.resources.pause_preview
import setiker.composeapp.generated.resources.play_preview
import setiker.composeapp.generated.resources.publisher_label
import setiker.composeapp.generated.resources.publisher_placeholder
import setiker.composeapp.generated.resources.video_pack_ask_ai
import setiker.composeapp.generated.resources.video_pack_build_grids
import setiker.composeapp.generated.resources.video_pack_find_frames
import setiker.composeapp.generated.resources.video_pack_generate
import setiker.composeapp.generated.resources.video_pack_max_duration
import setiker.composeapp.generated.resources.video_pack_prepare_preview
import setiker.composeapp.generated.resources.video_pack_prompt_label
import setiker.composeapp.generated.resources.video_pack_prompt_placeholder
import setiker.composeapp.generated.resources.video_pack_regenerate
import setiker.composeapp.generated.resources.video_pack_save
import setiker.composeapp.generated.resources.video_pack_subtitle
import setiker.composeapp.generated.resources.video_pack_title
import setiker.composeapp.generated.resources.video_preview
import util.decodeImageBitmap

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStickerPackScreen(
    state: VideoStickerPackState,
    onIntent: (VideoStickerPackIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.video_pack_title),
                onBackClick = null
            )
        },
        bottomBar = {
            VideoStickerPackBottomBar(
                state = state,
                onIntent = onIntent,
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        InteractionBlockedBox(
            blocked = state.isSaving,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(neubrutalScreenBackground())
        ) {
            VideoStickerPackContent(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .keyboardAwareScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun VideoStickerPackBottomBar(
    state: VideoStickerPackState,
    onIntent: (VideoStickerPackIntent) -> Unit,
    onBackClick: () -> Unit
) {
    val fabDescriptionRes = if (state.generatedPlan == null) {
        Res.string.video_pack_generate
    } else {
        Res.string.video_pack_regenerate
    }

    PackBottomBar(
        actionStatusText = if (state.isProcessing) {
            state.backgroundJobMessage ?: state.processingStep?.toUiLabel()
        } else {
            null
        },
        actions = {
            PackBottomBarIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back),
                onClick = onBackClick,
                enabled = !state.isProcessing
            )
            PackBottomBarIconButton(
                icon = if (state.isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (state.isPreviewPlaying) Res.string.pause_preview else Res.string.play_preview
                ),
                onClick = {
                    if (state.isPreviewPlaying) onIntent(VideoStickerPackIntent.PausePreview)
                    else onIntent(VideoStickerPackIntent.PlayPreview)
                },
                enabled = state.canPlayPreview
            )
            PackBottomBarIconButton(
                icon = Icons.Filled.Save,
                contentDescription = stringResource(Res.string.video_pack_save),
                onClick = { onIntent(VideoStickerPackIntent.SavePack) },
                enabled = state.canSave
            )
        },
        floatingActionButton = {
            PackBottomBarFab(
                icon = Icons.Filled.AutoAwesome,
                contentDescription = stringResource(fabDescriptionRes),
                onClick = {
                    if (state.generatedPlan == null) onIntent(VideoStickerPackIntent.Generate)
                    else onIntent(VideoStickerPackIntent.Regenerate)
                },
                enabled = state.canGenerate,
                isLoading = state.isProcessing
            )
        }
    )
}

@Composable
private fun VideoStickerPackContent(
    state: VideoStickerPackState,
    onIntent: (VideoStickerPackIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentEnabled = !state.isBlockingUi

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.video_pack_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = neubrutalMutedOnSurface()
        )
        Text(
            text = stringResource(Res.string.video_pack_max_duration),
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalSubtleOnSurface()
        )
        if (state.aiUsage != null || state.isLoadingAiUsage || state.aiUsageLoadFailed) {
            AiQuotaSummary(
                usage = state.aiUsage,
                isLoading = state.isLoadingAiUsage,
                hasError = state.aiUsageLoadFailed,
                highlightOperation = AiQuotaOperation.VIDEO_STICKER_PACK
            )
        }

        if (state.sourceDurationMs > 0L) {
            VideoPreviewSection(
                state = state,
                onIntent = onIntent,
                enabled = contentEnabled
            )
        } else {
            AppIllustrationImage(
                illustration = AppIllustration.VideoTools,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )
        }

        if (state.isProcessing) {
            VideoProcessingSection(state = state)
        }

        AppTextField(
            value = state.prompt,
            onValueChange = { onIntent(VideoStickerPackIntent.UpdatePrompt(it)) },
            label = stringResource(Res.string.video_pack_prompt_label),
            placeholder = stringResource(Res.string.video_pack_prompt_placeholder),
            enabled = contentEnabled
        )

        AppTextField(
            value = state.packName,
            onValueChange = { onIntent(VideoStickerPackIntent.UpdatePackName(it)) },
            label = stringResource(Res.string.pack_name_label),
            placeholder = stringResource(Res.string.pack_name_placeholder),
            enabled = contentEnabled
        )

        AppTextField(
            value = state.publisher,
            onValueChange = { onIntent(VideoStickerPackIntent.UpdatePublisher(it)) },
            label = stringResource(Res.string.publisher_label),
            placeholder = stringResource(Res.string.publisher_placeholder),
            enabled = contentEnabled
        )

        state.generatedPlan?.let { generatedPlan ->
            GeneratedPlanSection(
                state = state,
                generatedPlan = generatedPlan,
                contentEnabled = contentEnabled,
                onIntent = onIntent
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun VideoPreviewSection(
    state: VideoStickerPackState,
    onIntent: (VideoStickerPackIntent) -> Unit,
    enabled: Boolean
) {
    val previewBitmaps = rememberPreviewFrameBitmaps(state.previewFrames)
    val previewBitmap = state.currentPreviewPathResolved?.let { previewBitmaps[it] }
    val relativePreviewMs = (state.currentPreviewTimestampMs - state.selectedStartMs).coerceAtLeast(0L)

    VideoSourcePreview(
        previewBitmap = previewBitmap,
        previewLabelMs = relativePreviewMs,
        isLoading = state.isLoadingVideo || (state.currentPreviewPathResolved != null && previewBitmap == null)
    )

    LabeledSliderSection(
        label = "Selected clip range",
        supportingText = "${formatMs(state.selectedStartMs)} -> ${formatMs(state.selectedEndMs)}"
    ) {
        RangeSlider(
            value = state.selectedStartMs.toFloat()..state.selectedEndMs.toFloat(),
            onValueChange = { range ->
                onIntent(VideoStickerPackIntent.UpdateStart(range.start.toLong()))
                onIntent(VideoStickerPackIntent.UpdateEnd(range.endInclusive.toLong()))
            },
            valueRange = 0f..state.sourceDurationMs.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !state.isProcessing
        )
    }

    if (state.activePreviewFrames.size > 1) {
        LabeledSliderSection(
            label = "Preview position",
            supportingText = "${formatMs(relativePreviewMs)} / ${formatMs(state.selectedDurationMs)}"
        ) {
            Slider(
                value = state.currentPreviewIndex.toFloat(),
                onValueChange = { onIntent(VideoStickerPackIntent.ScrubPreviewTo(it.toInt())) },
                valueRange = 0f..(state.activePreviewFrames.size - 1).coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !state.isProcessing
            )
        }
    }
}

@Composable
private fun VideoSourcePreview(
    previewBitmap: ImageBitmap?,
    previewLabelMs: Long,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    NeubrutalStickerPreviewFrame(
        cornerRadius = NeubrutalCardRadius,
        modifier = modifier
    ) {
        when {
            previewBitmap != null -> {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = stringResource(Res.string.video_preview),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(NeubrutalCardRadius)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeubrutalBlack.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatMs(previewLabelMs),
                        color = NeubrutalWhite,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator(modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.loading_frames),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalSubtleOnSurface()
                    )
                }
            }

            else -> {
                Text(
                    text = stringResource(Res.string.loading_frames),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalSubtleOnSurface()
                )
            }
        }
    }
}

@Composable
private fun VideoProcessingSection(
    state: VideoStickerPackState
) {
    LinearProgressIndicator(
        progress = { state.processingProgress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth()
    )
    val processingLabel = state.backgroundJobMessage ?: state.processingStep?.toUiLabel().orEmpty()
    if (processingLabel.isNotBlank()) {
        Text(
            text = processingLabel,
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalSubtleOnSurface()
        )
    }
}

@Composable
private fun GeneratedPlanSection(
    state: VideoStickerPackState,
    generatedPlan: ResolvedVideoStickerPackPlan,
    contentEnabled: Boolean,
    onIntent: (VideoStickerPackIntent) -> Unit
) {
    Text(
        text = generatedPlan.plan.packTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    generatedPlan.plan.summary?.takeIf { it.isNotBlank() }?.let { summary ->
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalMutedOnSurface()
        )
    }
    StaticStickerSection(
        state = state,
        generatedPlan = generatedPlan,
        contentEnabled = contentEnabled,
        onIntent = onIntent
    )
    AnimatedStickerSection(
        state = state,
        generatedPlan = generatedPlan,
        contentEnabled = contentEnabled,
        onIntent = onIntent
    )
}

@Composable
private fun StaticStickerSection(
    state: VideoStickerPackState,
    generatedPlan: ResolvedVideoStickerPackPlan,
    contentEnabled: Boolean,
    onIntent: (VideoStickerPackIntent) -> Unit
) {
    Text(
        text = "Static stickers: ${generatedPlan.staticStickers.size}",
        style = MaterialTheme.typography.bodySmall,
        color = neubrutalMutedOnSurface()
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        generatedPlan.staticStickers.forEach { generated ->
            val key = generated.selectionKey()
            val isSelected = key in state.selectedStaticStickerKeys
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SelectableGeneratedStickerCard(
                    selected = isSelected,
                    enabled = contentEnabled,
                    onClick = { onIntent(VideoStickerPackIntent.ToggleStaticStickerSelection(key)) }
                ) {
                    StickerCard(
                        sticker = Sticker(
                            imageFile = generated.localPath,
                            decorations = generated.plan.decorations,
                            accessibilityText = generated.plan.accessibilityText
                        ),
                        onClick = {},
                        showDecorations = true,
                        modifier = Modifier.size(108.dp)
                    )
                }
                Text(
                    text = "${generated.plan.candidateId} • ${formatMs(generated.plan.timestampMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = neubrutalSubtleOnSurface()
                )
                if (generated.plan.emojis.isNotEmpty()) {
                    Text(
                        text = generated.plan.emojis.joinToString(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = neubrutalMutedOnSurface()
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStickerSection(
    state: VideoStickerPackState,
    generatedPlan: ResolvedVideoStickerPackPlan,
    contentEnabled: Boolean,
    onIntent: (VideoStickerPackIntent) -> Unit
) {
    if (generatedPlan.animatedStickers.isEmpty()) return

    Text(
        text = "Animated stickers: ${generatedPlan.animatedStickers.size}",
        style = MaterialTheme.typography.bodySmall,
        color = neubrutalMutedOnSurface()
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        generatedPlan.animatedStickers.forEachIndexed { index, animated ->
            val key = animated.selectionKey(index)
            val isSelected = key in state.selectedAnimatedStickerKeys
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnimatedVideoStickerPreviewCard(
                    animated = animated,
                    selected = isSelected,
                    enabled = contentEnabled,
                    onClick = { onIntent(VideoStickerPackIntent.ToggleAnimatedStickerSelection(key)) }
                )
                Text(
                    text = "Loop ${index + 1}: ${animated.timeline.size} frames at ${animated.plan.fps} fps",
                    style = MaterialTheme.typography.labelSmall,
                    color = neubrutalSubtleOnSurface()
                )
            }
        }
    }
}

@Composable
private fun AnimatedVideoStickerPreviewCard(
    animated: ResolvedVideoAnimatedSticker,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var frameIndex by remember(animated) { mutableIntStateOf(0) }
    LaunchedEffect(animated, frameIndex) {
        val duration = animated.timeline.getOrNull(frameIndex)?.frame?.durationMs ?: 83L
        delay(duration.coerceAtLeast(16L))
        frameIndex = if (animated.timeline.isEmpty()) 0 else (frameIndex + 1) % animated.timeline.size
    }
    val frame = animated.timeline.getOrNull(frameIndex) ?: return
    val decorations = animated.plan.baseDecorations + animated.plan.frameDecorations[frameIndex].orEmpty()

    SelectableGeneratedStickerCard(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        StickerCard(
            sticker = Sticker(
                imageFile = frame.localPath,
                decorations = decorations,
                accessibilityText = animated.plan.accessibilityText
            ),
            onClick = {},
            showDecorations = true,
            modifier = Modifier.size(108.dp)
        )
    }
}

@Composable
private fun SelectableGeneratedStickerCard(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = if (selected) AccentCoral else neubrutalBorderColor().copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .neubrutalBorderWithGloss(
                color = borderColor,
                cornerRadius = 18.dp,
                width = if (selected) 3.dp else NeubrutalBorderWidth,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun LabeledSliderSection(
    label: String? = null,
    supportingText: String? = null,
    content: @Composable () -> Unit
) {
    if (!label.isNullOrBlank()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = neubrutalMutedOnSurface()
        )
    }
    if (!supportingText.isNullOrBlank()) {
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalSubtleOnSurface()
        )
    }
    content()
}

@Composable
private fun rememberPreviewFrameBitmaps(
    frames: List<VideoStickerPackPreviewFrame>
): Map<String, ImageBitmap> {
    val cache = remember { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(frames) {
        for (frame in frames) {
            if (cache.containsKey(frame.filePath)) continue
            val bitmap = withContext(Dispatchers.Default) {
                runCatching { readFileBytes(frame.filePath) }
                    .getOrNull()
                    ?.let { decodeImageBitmap(it) }
            }
            if (bitmap != null) cache[frame.filePath] = bitmap
        }
    }
    return cache
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000.0
    return "${(totalSec * 10).toLong() / 10.0}s"
}

@Composable
private fun VideoStickerPackProcessingStep.toUiLabel(): String {
    val resId = when (this) {
        VideoStickerPackProcessingStep.FindingFrames -> Res.string.video_pack_find_frames
        VideoStickerPackProcessingStep.BuildingGrids -> Res.string.video_pack_build_grids
        VideoStickerPackProcessingStep.AskingAi -> Res.string.video_pack_ask_ai
        VideoStickerPackProcessingStep.PreparingPreview -> Res.string.video_pack_prepare_preview
        VideoStickerPackProcessingStep.Saving -> Res.string.video_pack_save
    }
    return stringResource(resId)
}
