package presentation.videostickerpack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import data.remote.readFileBytes
import domain.model.AiQuotaOperation
import domain.model.ResolvedVideoAnimatedSticker
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.Sticker
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import presentation.common.rememberReduceMotion
import presentation.components.AiQuotaSummary
import presentation.components.AppIllustration
import presentation.components.AppIllustrationImage
import presentation.components.AppSnackbarHostState
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.MorphGlyphIcon
import presentation.components.NeubrutalProgress
import presentation.components.NeubrutalRangeSlider
import presentation.components.NeubrutalSnackbarHost
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.StickerCard
import presentation.components.scaffoldBottomBarScroll
import presentation.icons.SetikerIcons
import presentation.icons.SetikerMorphs
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
import presentation.theme.neubrutalFill
import presentation.theme.NeubrutalBorderRole
import presentation.theme.neubrutalBorderFor
import presentation.theme.neubrutalBorderWidthFor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.animated_error_decode_failed
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.loading_frames
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder
import setiker.composeapp.generated.resources.pause_preview
import setiker.composeapp.generated.resources.play_preview
import setiker.composeapp.generated.resources.preview_position
import setiker.composeapp.generated.resources.publisher_label
import setiker.composeapp.generated.resources.publisher_placeholder
import setiker.composeapp.generated.resources.retry
import setiker.composeapp.generated.resources.video_pack_ask_ai
import setiker.composeapp.generated.resources.video_pack_ask_ai_hint
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
    snackbarHostState: AppSnackbarHostState = remember { AppSnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
        snackbarHost = { NeubrutalSnackbarHost(snackbarHostState) },
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
                    .scaffoldBottomBarScroll(rememberScrollState())
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
                icon = SetikerIcons.Back,
                contentDescription = stringResource(Res.string.back),
                onClick = onBackClick,
                enabled = !state.isProcessing
            )
            PackBottomBarIconButton(
                glyphMorph = SetikerMorphs.PlayPause,
                checked = state.isPreviewPlaying,
                contentDescription = stringResource(
                    if (state.isPreviewPlaying) Res.string.pause_preview else Res.string.play_preview
                ),
                onCheckedChange = { play ->
                    onIntent(
                        if (play) VideoStickerPackIntent.PlayPreview
                        else VideoStickerPackIntent.PausePreview
                    )
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
                icon = SetikerIcons.Sparkle,
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
            // Guidance belongs where the user is still deciding what to send, not above a player
            // that already answered the question.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppIllustrationImage(
                    illustration = AppIllustration.VideoTools,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp)
                )
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
            }
        }

        if (state.isProcessing) {
            VideoProcessingSection(state = state)
        }

        AppTextField(
            value = state.prompt,
            onValueChange = { onIntent(VideoStickerPackIntent.UpdatePrompt(it)) },
            label = stringResource(Res.string.video_pack_prompt_label),
            placeholder = stringResource(Res.string.video_pack_prompt_placeholder),
            imeAction = ImeAction.Next,
            enabled = contentEnabled
        )

        AppTextField(
            value = state.packName,
            onValueChange = { onIntent(VideoStickerPackIntent.UpdatePackName(it)) },
            label = stringResource(Res.string.pack_name_label),
            placeholder = stringResource(Res.string.pack_name_placeholder),
            imeAction = ImeAction.Next,
            enabled = contentEnabled
        )

        AppTextField(
            value = state.publisher,
            onValueChange = { onIntent(VideoStickerPackIntent.UpdatePublisher(it)) },
            label = stringResource(Res.string.publisher_label),
            placeholder = stringResource(Res.string.publisher_placeholder),
            imeAction = ImeAction.Done,
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
    val frameCount = state.activePreviewFrames.size
    val scrubProgress = if (frameCount > 1) {
        state.currentPreviewIndex.toFloat() / (frameCount - 1).toFloat()
    } else {
        null
    }

    VideoSourcePreview(
        previewBitmap = previewBitmap,
        previewLabelMs = relativePreviewMs,
        isLoading = state.isLoadingVideo || (state.currentPreviewPathResolved != null && previewBitmap == null),
        retryUnavailable = state.previewUnavailable,
        onRetry = { onIntent(VideoStickerPackIntent.RetryPreview) },
        isPlaying = state.isPreviewPlaying,
        // Tapping the frame is the affordance: the bar button still works, but nobody should have
        // to hunt for a control below a video they are already looking at.
        onTogglePlay = if (state.canPlayPreview && enabled) {
            {
                onIntent(
                    if (state.isPreviewPlaying) VideoStickerPackIntent.PausePreview
                    else VideoStickerPackIntent.PlayPreview
                )
            }
        } else {
            null
        },
        scrubProgress = scrubProgress,
        onScrub = if (scrubProgress != null && enabled) {
            { fraction ->
                onIntent(
                    VideoStickerPackIntent.ScrubPreviewTo(
                        (fraction * (frameCount - 1)).roundToInt().coerceIn(0, frameCount - 1)
                    )
                )
            }
        } else {
            null
        }
    )

    LabeledSliderSection(
        label = "Selected clip range",
        supportingText = "${formatMs(state.selectedStartMs)} -> ${formatMs(state.selectedEndMs)}"
    ) {
        NeubrutalRangeSlider(
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
}

@Composable
private fun VideoSourcePreview(
    previewBitmap: ImageBitmap?,
    previewLabelMs: Long,
    isLoading: Boolean,
    retryUnavailable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onTogglePlay: (() -> Unit)? = null,
    scrubProgress: Float? = null,
    onScrub: ((Float) -> Unit)? = null
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
                        .clip(RoundedCornerShape(NeubrutalCardRadius))
                        .then(
                            if (onTogglePlay != null) {
                                Modifier.clickable(
                                    onClickLabel = stringResource(
                                        if (isPlaying) Res.string.pause_preview else Res.string.play_preview
                                    ),
                                    onClick = onTogglePlay
                                )
                            } else {
                                Modifier
                            }
                        ),
                    contentScale = ContentScale.Crop
                )
                if (!isPlaying && onTogglePlay != null) {
                    PlayAffordance(modifier = Modifier.align(Alignment.Center))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .neubrutalFill(color = NeubrutalBlack.copy(alpha = 0.6f), cornerRadius = 8.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatMs(previewLabelMs),
                        color = NeubrutalWhite,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                // The playhead lives on the frame it points at, so the clip range stays the only
                // slider on the screen.
                if (scrubProgress != null && onScrub != null) {
                    PreviewScrubber(
                        progress = scrubProgress,
                        onScrub = onScrub,
                        description = stringResource(Res.string.preview_position),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
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

            retryUnavailable -> {
                // The device could not read a single frame from this source; without this branch the
                // placeholder below would animate forever.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.animated_error_decode_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalSubtleOnSurface()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.retry),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeubrutalWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .neubrutalFill(color = NeubrutalBlack, cornerRadius = 8.dp)
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
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

/**
 * The tap target, made visible.
 *
 * A frame that does nothing until you find a small bar button reads as a broken player. This badge
 * says "this is a video, tap it" without adding a second control: the whole frame is the button, so
 * the badge can stay decorative.
 */
@Composable
private fun PlayAffordance(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = 36.dp,
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .neubrutalFill(color = AccentCoral, cornerRadius = 99.dp)
            .neubrutalBorderWithGloss(
                color = NeubrutalBlack,
                cornerRadius = 36.dp,
                shape = CircleShape,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
            ),
        contentAlignment = Alignment.Center
    ) {
        MorphGlyphIcon(
            morph = SetikerMorphs.PlayPause,
            checked = false,
            size = 30.dp,
            tint = NeubrutalWhite
        )
    }
}
/**
 * The playhead, drawn on the frame it points at.
 *
 * There used to be a second full-width slider under the clip range, so the screen read as two
 * stacked sliders even though they did different things. Scrubbing belongs on the thing being
 * scrubbed: the timeline below stays the only slider, and this is the thin track that moves a
 * frame at a time.
 */
@Composable
private fun PreviewScrubber(
    progress: Float,
    onScrub: (Float) -> Unit,
    description: String,
    modifier: Modifier = Modifier
) {
    val widthState = remember { mutableStateOf(1f) }
    val clamped = progress.coerceIn(0f, 1f)

    fun fraction(x: Float): Float = (x / widthState.value).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .onSizeChanged { widthState.value = it.width.toFloat().coerceAtLeast(1f) }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> onScrub(fraction(offset.x)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onScrub(fraction(offset.x)) },
                    onHorizontalDrag = { change, _ -> onScrub(fraction(change.position.x)) }
                )
            }
    ) {
        val trackHeight = 4.dp.toPx()
        val top = (size.height - trackHeight) / 2f
        val corner = CornerRadius(trackHeight / 2f)
        drawRoundRect(
            color = NeubrutalBlack.copy(alpha = 0.45f),
            topLeft = Offset(0f, top),
            size = Size(size.width, trackHeight),
            cornerRadius = corner
        )
        val filledWidth = size.width * clamped
        drawRoundRect(
            color = AccentCoral,
            topLeft = Offset(0f, top),
            size = Size(filledWidth, trackHeight),
            cornerRadius = corner
        )
        val half = trackHeight
        val knobX = filledWidth.coerceIn(half, (size.width - half).coerceAtLeast(half))
        val knob = Offset(x = knobX, y = size.height / 2f)
        drawCircle(color = NeubrutalWhite, radius = trackHeight, center = knob)
        drawCircle(
            color = NeubrutalBlack,
            radius = trackHeight,
            center = knob,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}
@Composable
private fun VideoProcessingSection(
    state: VideoStickerPackState
) {
    NeubrutalProgress(progress = state.processingProgress.coerceIn(0f, 1f))
    val processingLabel = state.backgroundJobMessage ?: state.processingStep?.toUiLabel().orEmpty()
    if (processingLabel.isNotBlank()) {
        Text(
            text = processingLabel,
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalSubtleOnSurface()
        )
    }
    if (
        state.backgroundJobMessage == null &&
        state.processingStep == VideoStickerPackProcessingStep.AskingAi
    ) {
        Text(
            text = stringResource(Res.string.video_pack_ask_ai_hint),
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
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(animated, frameIndex, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        val duration = animated.timeline.getOrNull(frameIndex)?.frame?.durationMs ?: 83L
        delay(duration.milliseconds.coerceAtLeast(16.milliseconds))
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
    val borderColor = if (selected) {
        neubrutalBorderFor(NeubrutalBorderRole.Selected)
    } else {
        neubrutalBorderColor().copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier
            .neubrutalBorderWithGloss(
                color = borderColor,
                cornerRadius = 18.dp,
                width = if (selected) {
                    neubrutalBorderWidthFor(NeubrutalBorderRole.Selected)
                } else {
                    NeubrutalBorderWidth
                },
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

@Preview
@Composable
private fun VideoStickerPackScreenPreview() {
    MaterialTheme {
        VideoStickerPackScreen(
            state = VideoStickerPackState(),
            onIntent = {},
            onBackClick = {}
        )
    }
}

