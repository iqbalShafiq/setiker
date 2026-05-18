package presentation.videostickerpack

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.StickerCard
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder
import setiker.composeapp.generated.resources.publisher_label
import setiker.composeapp.generated.resources.publisher_placeholder
import setiker.composeapp.generated.resources.video_pack_ask_ai
import setiker.composeapp.generated.resources.video_pack_build_grids
import setiker.composeapp.generated.resources.video_pack_find_frames
import setiker.composeapp.generated.resources.video_pack_generate
import setiker.composeapp.generated.resources.video_pack_max_duration
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
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.video_pack_title),
                onBackClick = onBackClick
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

            if (state.generatedStickers.isNotEmpty()) {
                Text(
                    text = "Generated stickers: ${state.generatedStickers.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.generatedStickers.forEach { generated ->
                        StickerCard(
                            sticker = Sticker(imageFile = generated.localPath),
                            onClick = {},
                            modifier = Modifier.size(108.dp)
                        )
                    }
                }
            }

            AppPrimaryButton(
                text = stringResource(
                    if (state.generatedStickers.isEmpty()) Res.string.video_pack_generate
                    else Res.string.video_pack_regenerate
                ),
                onClick = {
                    if (state.generatedStickers.isEmpty()) onIntent(VideoStickerPackIntent.Generate)
                    else onIntent(VideoStickerPackIntent.Regenerate)
                },
                enabled = state.canGenerate
            )
            AppPrimaryButton(
                text = stringResource(Res.string.video_pack_save),
                onClick = { onIntent(VideoStickerPackIntent.SavePack) },
                enabled = state.canSave
            )
            AppSecondaryButton(
                text = stringResource(Res.string.cancel),
                onClick = { onIntent(VideoStickerPackIntent.Cancel) },
                enabled = !state.isProcessing
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
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
    }
    return stringResource(resId)
}
