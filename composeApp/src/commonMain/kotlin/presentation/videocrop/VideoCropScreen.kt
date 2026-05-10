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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import domain.model.CropTransform
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.components.MediaPreviewBottomBar
import presentation.components.NeubrutalStickerPreviewFrame
import presentation.components.ProgressDialog
import presentation.theme.AccentCoral
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
            NeubrutalStickerPreviewFrame {
                val previewPath = state.currentPreviewPath
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
                    previewPath != null -> {
                        VideoCropTransformableImage(
                            imagePath = previewPath,
                            transform = state.transform,
                            onUpdate = { scale, ox, oy ->
                                onIntent(VideoCropIntent.UpdateScale(scale))
                                onIntent(VideoCropIntent.UpdateOffset(ox, oy))
                            }
                        )
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
    imagePath: String,
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
            painter = rememberAsyncImagePainter(imagePath),
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

        // Square crop overlay (always exactly the visible 1:1 box; what the user sees IS the
        // saved 512×512 frame, so we just draw a thin border + grid for guidance).
        val gridLine = NeubrutalWhite.copy(alpha = 0.6f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        color = NeubrutalWhite,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height),
                        style = Stroke(width = 2.dp.toPx())
                    )
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
                .border(2.dp, border, CircleShape)
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
