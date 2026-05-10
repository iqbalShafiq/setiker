package presentation.videotrim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.components.MediaPreviewBottomBar
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.ScreenSectionTitle
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.animation_duration_cap
import setiker.composeapp.generated.resources.continue_action
import setiker.composeapp.generated.resources.frames_per_second
import setiker.composeapp.generated.resources.loading_frames
import setiker.composeapp.generated.resources.source_video
import setiker.composeapp.generated.resources.speed
import setiker.composeapp.generated.resources.trim
import setiker.composeapp.generated.resources.trim_duration_overlay
import setiker.composeapp.generated.resources.trim_video_title
import setiker.composeapp.generated.resources.video_preview

@Composable
fun VideoTrimScreen(
    state: VideoTrimState,
    onIntent: (VideoTrimIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.trim_video_title),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                MediaPreviewBottomBar(
                    primaryIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    primaryDescription = stringResource(Res.string.continue_action),
                    onPrimary = { onIntent(VideoTrimIntent.Confirm) },
                    primaryEnabled = !state.isExtracting && state.effectiveTrimMs > 0,
                    onCancel = { onIntent(VideoTrimIntent.Cancel) },
                    cancelEnabled = !state.isExtracting,
                    isPlaying = state.isPlaying,
                    playEnabled = state.canPlay,
                    onTogglePlay = {
                        if (state.isPlaying) onIntent(VideoTrimIntent.PausePreview)
                        else onIntent(VideoTrimIntent.PlayPreview)
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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ScreenSectionTitle(text = stringResource(Res.string.source_video))
                Spacer(modifier = Modifier.height(10.dp))

                NeubrutalStickerPreviewFrame(
                    cornerRadius = 16.dp,
                    shadowOffsetX = 3.dp,
                    shadowOffsetY = 3.dp
                ) {
                    val previewPath = state.currentPreviewPath
                    when {
                        previewPath != null -> {
                            AsyncImage(
                                model = previewPath,
                                contentDescription = stringResource(Res.string.video_preview),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        state.thumbnails.isEmpty() -> {
                            // Thumbnails are still being extracted from the source video.
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LoadingIndicator(modifier = Modifier.height(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(Res.string.loading_frames),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = neubrutalSubtleOnSurface()
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeubrutalBlack.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.trim_duration_overlay,
                                formatMs(state.effectiveTrimMs),
                                formatMs(state.maxAllowedTrimMs)
                            ),
                            color = NeubrutalWhite,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                ScreenSectionTitle(text = stringResource(Res.string.trim))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${formatMs(state.trimStartMs)} → ${formatMs(state.trimEndMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = neubrutalMutedOnSurface()
                )
                if (state.videoDurationMs > 0) {
                    val rangeMax = state.videoDurationMs.toFloat()
                    RangeSlider(
                        value = state.trimStartMs.toFloat()..state.trimEndMs.toFloat(),
                        onValueChange = { range ->
                            onIntent(VideoTrimIntent.UpdateTrimStart(range.start.toLong()))
                            onIntent(VideoTrimIntent.UpdateTrimEnd(range.endInclusive.toLong()))
                        },
                        valueRange = 0f..rangeMax,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.animation_duration_cap,
                        (state.maxAllowedTrimMs / 1000).toInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalSubtleOnSurface()
                )

                Spacer(modifier = Modifier.height(20.dp))
                ScreenSectionTitle(text = stringResource(Res.string.frames_per_second))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoTrimState.FPS_OPTIONS.forEach { fps ->
                        FilterChip(
                            selected = state.fps == fps,
                            onClick = { onIntent(VideoTrimIntent.UpdateFps(fps)) },
                            label = { Text("$fps fps") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                ScreenSectionTitle(text = stringResource(Res.string.speed))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoTrimState.SPEED_OPTIONS.forEach { speed ->
                        FilterChip(
                            selected = state.speed == speed,
                            onClick = { onIntent(VideoTrimIntent.UpdateSpeed(speed)) },
                            label = { Text("${formatSpeed(speed)}x") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!state.errorMessage.isNullOrBlank()) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000.0
    return "${(totalSec * 10).toLong() / 10.0}s"
}

private fun formatSpeed(speed: Float): String {
    return ((speed * 10).toInt() / 10.0).toString()
}
