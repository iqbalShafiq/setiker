package presentation.videocrop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import data.remote.readFileBytes
import domain.model.CropTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.components.MediaPreviewBottomBar
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.ProgressDialog
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.apply
import setiker.composeapp.generated.resources.encoding_progress_title
import setiker.composeapp.generated.resources.extracting_frames
import setiker.composeapp.generated.resources.extracting_preview
import setiker.composeapp.generated.resources.flip_horizontal
import setiker.composeapp.generated.resources.reset
import setiker.composeapp.generated.resources.rotate_left
import setiker.composeapp.generated.resources.rotate_right
import setiker.composeapp.generated.resources.video_crop_hint
import setiker.composeapp.generated.resources.video_crop_title
import setiker.composeapp.generated.resources.zoom
import util.decodeImageBitmap

@Composable
fun VideoCropScreen(
    state: VideoCropState,
    onIntent: (VideoCropIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.video_crop_title),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            MediaPreviewBottomBar(
                primaryIcon = Icons.Filled.Check,
                primaryDescription = stringResource(Res.string.apply),
                onPrimary = { onIntent(VideoCropIntent.Apply) },
                primaryEnabled = state.currentPreviewPath != null && !state.isApplying,
                onCancel = { onIntent(VideoCropIntent.Cancel) },
                cancelEnabled = !state.isApplying,
                isPlaying = state.isPlaying,
                playEnabled = state.canPlay && !state.isApplying,
                onTogglePlay = {
                    if (state.isPlaying) onIntent(VideoCropIntent.PausePreview)
                    else onIntent(VideoCropIntent.PlayPreview)
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
        ) {
            // Decode every preview frame to an ImageBitmap and keep them in a
            // path-keyed map. Same approach the animated editor uses for buttery
            // smooth playback — once decoded the swap between frames during play
            // is a constant-time map lookup and never goes through Coil/disk.
            val previewBitmaps = rememberPreviewFrameBitmaps(state.previewFrames)
            val currentBitmap = state.currentPreviewPath?.let { previewBitmaps[it] }

            NeubrutalStickerPreviewFrame {
                when {
                    state.isLoadingPreview && state.previewFrames.isEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.extracting_preview),
                                style = MaterialTheme.typography.bodySmall,
                                color = neubrutalSubtleOnSurface()
                            )
                        }
                    }
                    currentBitmap != null -> {
                        VideoCropTransformableImage(
                            bitmap = currentBitmap,
                            transform = state.transform,
                            onUpdate = { scale, ox, oy ->
                                onIntent(VideoCropIntent.UpdateScale(scale))
                                onIntent(VideoCropIntent.UpdateOffset(ox, oy))
                            }
                        )
                    }
                    state.currentPreviewPath != null -> {
                        // Bitmap not decoded yet — keep the loader visible while
                        // it streams in instead of flashing the error placeholder.
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.extracting_preview),
                                style = MaterialTheme.typography.bodySmall,
                                color = neubrutalSubtleOnSurface()
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = neubrutalSubtleOnSurface(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.video_crop_hint),
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.zoom),
                style = MaterialTheme.typography.labelLarge,
                color = neubrutalMutedOnSurface()
            )
            Slider(
                value = state.transform.scale,
                onValueChange = { onIntent(VideoCropIntent.UpdateScale(it)) },
                valueRange = CropTransform.MIN_SCALE..CropTransform.MAX_SCALE,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CropToolButton(
                    icon = Icons.AutoMirrored.Filled.RotateLeft,
                    label = stringResource(Res.string.rotate_left),
                    onClick = { onIntent(VideoCropIntent.RotateLeft) }
                )
                CropToolButton(
                    icon = Icons.AutoMirrored.Filled.RotateRight,
                    label = stringResource(Res.string.rotate_right),
                    onClick = { onIntent(VideoCropIntent.RotateRight) }
                )
                CropToolButton(
                    icon = Icons.Filled.Flip,
                    label = stringResource(Res.string.flip_horizontal),
                    onClick = { onIntent(VideoCropIntent.FlipHorizontal) }
                )
                CropToolButton(
                    icon = Icons.Filled.Refresh,
                    label = stringResource(Res.string.reset),
                    onClick = { onIntent(VideoCropIntent.Reset) }
                )
            }
        }
    }

    if (state.isApplying) {
        // Real progress: forwarded from the storage layer's onProgress callback so
        // the dialog reflects the actual frame extraction work, not a fake animation.
        val label = state.applyProgressLabel ?: stringResource(Res.string.extracting_frames)
        ProgressDialog(
            title = stringResource(Res.string.encoding_progress_title),
            progress = state.applyProgress,
            progressLabel = label
        )
    }
}

@Composable
private fun VideoCropTransformableImage(
    bitmap: ImageBitmap,
    transform: CropTransform,
    onUpdate: (scale: Float, offsetXNorm: Float, offsetYNorm: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentScale by rememberUpdatedState(transform.scale)
    val currentOffsetX by rememberUpdatedState(transform.offsetXNorm)
    val currentOffsetY by rememberUpdatedState(transform.offsetYNorm)
    val currentOnUpdate by rememberUpdatedState(onUpdate)

    Box(
        modifier = modifier
            .fillMaxSize()
            // Clip the transformable area to the outer frame's rounded corners so
            // the dragged/zoomed image never spills past the neubrutal frame edge.
            // We rely solely on the outer NeubrutalStickerPreviewFrame for the
            // visible border — drawing a second white rect here was what made the
            // preview look like it had a double border with weird radii.
            .clip(RoundedCornerShape(NeubrutalSmallRadius))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val boxW = size.width.toFloat().coerceAtLeast(1f)
                    val boxH = size.height.toFloat().coerceAtLeast(1f)
                    currentOnUpdate(
                        currentScale * zoom,
                        currentOffsetX + pan.x / boxW,
                        currentOffsetY + pan.y / boxH
                    )
                }
            }
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val sx = transform.scale * if (transform.flipHorizontal) -1f else 1f
                    val sy = transform.scale * if (transform.flipVertical) -1f else 1f
                    scaleX = sx
                    scaleY = sy
                    rotationZ = transform.rotation
                    translationX = transform.offsetXNorm * size.width
                    translationY = transform.offsetYNorm * size.height
                },
            contentScale = ContentScale.Fit
        )

        // Rule-of-thirds grid only — no rectangle border. The outer
        // NeubrutalStickerPreviewFrame already provides the visible 1:1 boundary,
        // and what the user sees IS the saved 512×512 frame.
        val gridLine = NeubrutalWhite.copy(alpha = 0.6f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val third = size.width / 3f
                    repeat(2) { i ->
                        drawLine(
                            color = gridLine,
                            start = Offset((i + 1) * third, 0f),
                            end = Offset((i + 1) * third, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridLine,
                            start = Offset(0f, (i + 1) * third),
                            end = Offset(size.width, (i + 1) * third),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
        )
    }
}

/**
 * Decode every preview frame extracted by [VideoCropViewModel] to an in-memory
 * [ImageBitmap] keyed by file path. Re-runs are idempotent (already-decoded paths
 * are skipped), so even though [VideoCropViewModel] emits frames incrementally
 * we only pay decode cost once per frame.
 */
@Composable
private fun rememberPreviewFrameBitmaps(
    frames: List<VideoCropPreviewFrame>
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

// MARK: - Previews

@Preview
@Composable
private fun VideoCropScreenPreview() {
    MaterialTheme {
        VideoCropScreen(
            state = VideoCropState(
                videoPath = "",
                transform = CropTransform(scale = 1.2f)
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun VideoCropScreenProcessingPreview() {
    MaterialTheme {
        VideoCropScreen(
            state = VideoCropState(
                videoPath = "",
                isApplying = true,
                applyProgress = 0.5f,
                applyProgressLabel = "Extracting frames"
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Composable
private fun CropToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val surface = neubrutalCardSurface()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .neubrutalShadow(2.dp, 2.dp, 24.dp, shadow)
                .clip(CircleShape)
                .background(surface)
                .border(NeubrutalBorderWidth, border, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AccentCoral,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = neubrutalSubtleOnSurface()
        )
    }
}
