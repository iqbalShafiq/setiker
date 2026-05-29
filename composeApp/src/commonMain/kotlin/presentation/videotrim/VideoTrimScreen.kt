package presentation.videotrim

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import data.remote.readFileBytes
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppTopBar
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.MediaPreviewBottomBar
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.ScreenSectionTitle
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.animation_duration_cap
import setiker.composeapp.generated.resources.continue_action
import setiker.composeapp.generated.resources.frames_per_second
import setiker.composeapp.generated.resources.loading_frames
import setiker.composeapp.generated.resources.processing
import setiker.composeapp.generated.resources.source_video
import setiker.composeapp.generated.resources.speed
import setiker.composeapp.generated.resources.trim
import setiker.composeapp.generated.resources.trim_video_title
import setiker.composeapp.generated.resources.video_preview
import util.decodeImageBitmap

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
                    primaryLoading = state.isExtracting,
                    actionStatusText = if (state.isExtracting) stringResource(Res.string.processing) else null,
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
            InteractionBlockedBox(
                blocked = state.isExtracting,
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                ScreenSectionTitle(text = stringResource(Res.string.source_video))
                Spacer(modifier = Modifier.height(10.dp))

                // Pre-decode each thumbnail to an in-memory ImageBitmap. This is the
                // same trick AnimatedEditorScreen uses for its smooth playback — once
                // a frame is in this map, swapping it during playback is instant and
                // never reloads from disk, which is what caused the previous flicker.
                val thumbnailBitmaps = rememberThumbnailBitmaps(state.thumbnails)
                val previewBitmap = state.currentPreviewPath?.let { thumbnailBitmaps[it] }

                NeubrutalStickerPreviewFrame(
                    cornerRadius = NeubrutalCardRadius,
                    shadowOffsetX = NeubrutalShadowOffset,
                    shadowOffsetY = NeubrutalShadowOffset
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
                        }
                        else -> {
                            // Thumbnails are still being extracted or decoded.
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
                        // Just the chosen animation duration — the slider already shows
                        // the start→end range and the cap line below shows the maximum,
                        // so a "4.4s / 10.0s" overlay was duplicating info and made the
                        // numbers feel inconsistent with the slider readout.
                        Text(
                            text = formatMs(state.effectiveTrimMs),
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
}

// MARK: - Previews

@Preview
@Composable
private fun VideoTrimScreenPreview() {
    MaterialTheme {
        VideoTrimScreen(
            state = VideoTrimState(
                videoPath = "",
                videoDurationMs = 10000L,
                trimStartMs = 2000L,
                trimEndMs = 8000L,
                fps = 15,
                speed = 1f
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun VideoTrimScreenLoadingPreview() {
    MaterialTheme {
        VideoTrimScreen(
            state = VideoTrimState(isLoading = true),
            onIntent = {},
            onBackClick = {}
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000.0
    return "${(totalSec * 10).toLong() / 10.0}s"
}

private fun formatSpeed(speed: Float): String {
    return ((speed * 10).toInt() / 10.0).toString()
}

/**
 * Decode every extracted thumbnail to an [ImageBitmap] off the main thread and keep
 * them in a snapshot-backed map keyed by absolute file path. Re-running the effect
 * (e.g. when new thumbnails stream in incrementally) skips paths that were already
 * decoded, so we never repeat work. Cycling through frames during playback then
 * becomes an O(1) map lookup with zero disk or Coil involvement, eliminating the
 * "flicker / glitch" the user reported when each frame swap re-triggered a Coil
 * disk load.
 */
@Composable
private fun rememberThumbnailBitmaps(
    thumbnails: List<VideoTrimThumbnail>
): Map<String, ImageBitmap> {
    val cache = remember { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(thumbnails) {
        for (thumb in thumbnails) {
            if (cache.containsKey(thumb.filePath)) continue
            val bitmap = withContext(Dispatchers.Default) {
                runCatching { readFileBytes(thumb.filePath) }
                    .getOrNull()
                    ?.let { decodeImageBitmap(it) }
            }
            if (bitmap != null) cache[thumb.filePath] = bitmap
        }
    }
    return cache
}
