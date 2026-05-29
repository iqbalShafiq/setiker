package presentation.videostickerpack

import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.ResolvedVideoAnimatedSticker
import domain.model.Sticker
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppPrimaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.InteractionBlockedBox
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.StickerCard
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import presentation.theme.neubrutalBorderColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStickerPackScreen(
    state: VideoStickerPackState,
    onIntent: (VideoStickerPackIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val contentEnabled = !state.isBlockingUi
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.video_pack_title),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            PackBottomBar(
                actionStatusText = if (state.isSaving) stringResource(Res.string.video_pack_save) else null,
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        onClick = onBackClick,
                        enabled = !state.isProcessing
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Filled.Check,
                        contentDescription = stringResource(Res.string.video_pack_save),
                        onClick = { onIntent(VideoStickerPackIntent.SavePack) },
                        enabled = state.canSave,
                        isLoading = state.isSaving
                    )
                }
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

            if (state.sourceDurationMs > 0L) {
                Text(
                    text = "${formatMs(state.selectedStartMs)} -> ${formatMs(state.selectedEndMs)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                RangeSlider(
                    value = state.selectedStartMs.toFloat()..state.selectedEndMs.toFloat(),
                    onValueChange = { range ->
                        onIntent(VideoStickerPackIntent.UpdateStart(range.start.toLong()))
                        onIntent(VideoStickerPackIntent.UpdateEnd(range.endInclusive.toLong()))
                    },
                    valueRange = 0f..state.sourceDurationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = contentEnabled && !state.isProcessing
                )
            }

            if (state.isProcessing) {
                LinearProgressIndicator(
                    progress = { state.processingProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                val processingLabel = state.processingStep?.toUiLabel() ?: ""
                if (processingLabel.isNotBlank()) {
                    Text(
                        text = processingLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalSubtleOnSurface()
                    )
                }
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

            val generatedPlan = state.generatedPlan
            if (generatedPlan != null) {
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
                        val key = "static:${generated.plan.candidateId}:${generated.plan.timestampMs}:${generated.localPath}"
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
                if (generatedPlan.animatedStickers.isNotEmpty()) {
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
                            val key = "animated:$index:${animated.plan.timeline.firstOrNull()?.timestampMs ?: 0}:${animated.plan.timeline.lastOrNull()?.timestampMs ?: 0}"
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
            }

            AppPrimaryButton(
                text = stringResource(
                    if (state.generatedPlan == null) Res.string.video_pack_generate
                    else Res.string.video_pack_regenerate
                ),
                onClick = {
                    if (state.generatedPlan == null) onIntent(VideoStickerPackIntent.Generate)
                    else onIntent(VideoStickerPackIntent.Regenerate)
                },
                enabled = state.canGenerate
            )
                Spacer(modifier = Modifier.height(20.dp))
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
            .border(
                width = if (selected) 3.dp else NeubrutalBorderWidth,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
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
