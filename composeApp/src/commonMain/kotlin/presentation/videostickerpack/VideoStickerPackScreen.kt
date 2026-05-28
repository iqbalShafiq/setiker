package presentation.videostickerpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import data.remote.readFileBytes
import domain.model.Sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.components.StickerCard
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.loading_frames
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
            PackBottomBar(
                actionStatusText = state.processingStep?.toUiLabel()?.takeIf { state.isProcessing },
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        onClick = onBackClick,
                        enabled = !state.isProcessing
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
                        contentDescription = stringResource(
                            if (state.generatedPlan == null) Res.string.video_pack_generate
                            else Res.string.video_pack_regenerate
                        ),
                        onClick = {
                            if (state.generatedPlan == null) onIntent(VideoStickerPackIntent.Generate)
                            else onIntent(VideoStickerPackIntent.Regenerate)
                        },
                        enabled = state.canGenerate,
                        isLoading = state.isProcessing && state.processingStep != VideoStickerPackProcessingStep.Saving
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                VideoSourcePreview(
                    previewFramePath = state.previewFramePath,
                    durationMs = state.selectedDurationMs,
                    modifier = Modifier.fillMaxWidth()
                )
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
                    enabled = !state.isProcessing
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
                placeholder = stringResource(Res.string.video_pack_prompt_placeholder)
            )

            AppTextField(
                value = state.packName,
                onValueChange = { onIntent(VideoStickerPackIntent.UpdatePackName(it)) },
                label = stringResource(Res.string.pack_name_label),
                placeholder = stringResource(Res.string.pack_name_placeholder)
            )
            AppTextField(
                value = state.publisher,
                onValueChange = { onIntent(VideoStickerPackIntent.UpdatePublisher(it)) },
                label = stringResource(Res.string.publisher_label),
                placeholder = stringResource(Res.string.publisher_placeholder)
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    generatedPlan.animatedStickers.forEachIndexed { index, animated ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Loop ${index + 1}: ${animated.timeline.size} frames at ${animated.plan.fps} fps",
                                style = MaterialTheme.typography.bodySmall,
                                color = neubrutalSubtleOnSurface()
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                animated.timeline.forEachIndexed { frameIndex, frame ->
                                    val decorations = animated.plan.baseDecorations +
                                        animated.plan.frameDecorations[frameIndex].orEmpty()
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        StickerCard(
                                            sticker = Sticker(
                                                imageFile = frame.localPath,
                                                decorations = decorations,
                                                accessibilityText = animated.plan.accessibilityText
                                            ),
                                            onClick = {},
                                            showDecorations = true,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Text(
                                            text = formatMs(frame.frame.timestampMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = neubrutalSubtleOnSurface()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun VideoSourcePreview(
    previewFramePath: String?,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberPreviewBitmap(previewFramePath)

    NeubrutalStickerPreviewFrame(
        cornerRadius = NeubrutalCardRadius,
        modifier = modifier
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap,
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
                        text = formatMs(durationMs),
                        color = NeubrutalWhite,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            else -> {
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
        }
    }
}

@Composable
private fun rememberPreviewBitmap(path: String?): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = null
        if (path.isNullOrBlank()) return@LaunchedEffect
        bitmap = withContext(Dispatchers.Default) {
            runCatching { readFileBytes(path) }
                .getOrNull()
                ?.let { decodeImageBitmap(it) }
        }
    }
    return bitmap
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
