package presentation.crop

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.apply_crop
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.crop_image_title
import setiker.composeapp.generated.resources.flip_horizontal
import setiker.composeapp.generated.resources.image_to_crop
import setiker.composeapp.generated.resources.no_image_selected
import setiker.composeapp.generated.resources.reset
import setiker.composeapp.generated.resources.rotate_left
import setiker.composeapp.generated.resources.rotate_right
import setiker.composeapp.generated.resources.zoom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    state: CropState,
    onIntent: (CropIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.crop_image_title),
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeubrutalBg
    ) { innerPadding ->
        if (state.isProcessing) {
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
            ) {
                // Crop Area (Neubrutal Frame)
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
                        CropImagePreview(
                            imagePath = state.imagePath,
                            rotation = state.rotation,
                            scale = state.scale,
                            offsetX = state.offsetX,
                            offsetY = state.offsetY,
                            isFlippedHorizontal = state.isFlippedHorizontal,
                            isFlippedVertical = state.isFlippedVertical,
                            onTransform = { scale, offsetX, offsetY ->
                                onIntent(CropIntent.UpdateScale(scale))
                                onIntent(CropIntent.UpdateOffset(offsetX, offsetY))
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.no_image_selected),
                            style = MaterialTheme.typography.bodyLarge,
                            color = NeubrutalGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Zoom Slider
                Text(
                    text = stringResource(Res.string.zoom),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = NeubrutalBlack
                )

                Slider(
                    value = state.scale,
                    onValueChange = { onIntent(CropIntent.UpdateScale(it)) },
                    valueRange = 0.5f..3f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = stringResource(Res.string.rotate_left),
                        isSelected = false,
                        onClick = { onIntent(CropIntent.RotateLeft) }
                    )
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = stringResource(Res.string.rotate_right),
                        isSelected = false,
                        onClick = { onIntent(CropIntent.RotateRight) }
                    )
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = stringResource(Res.string.flip_horizontal),
                        isSelected = false,
                        onClick = { onIntent(CropIntent.FlipHorizontal) }
                    )
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = stringResource(Res.string.reset),
                        isSelected = false,
                        onClick = { onIntent(CropIntent.Reset) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Apply Button
                AppPrimaryButton(
                    text = stringResource(Res.string.apply_crop),
                    onClick = { onIntent(CropIntent.ApplyCrop) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun NeubrutalToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .neubrutalShadow(
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    cornerRadius = 24.dp,
                    color = NeubrutalBlack
                )
                .clip(CircleShape)
                .background(if (isSelected) AccentCoral else NeubrutalWhite)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) NeubrutalWhite else NeubrutalBlack
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NeubrutalGray
        )
    }
}

@Composable
private fun CropImagePreview(
    imagePath: String,
    rotation: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    isFlippedHorizontal: Boolean,
    isFlippedVertical: Boolean,
    onTransform: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use rememberUpdatedState to always have latest values in gesture handler
    val currentScale by rememberUpdatedState(scale)
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentOnTransform by rememberUpdatedState(onTransform)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    currentOnTransform(
                        currentScale * zoom,
                        currentOffsetX + pan.x,
                        currentOffsetY + pan.y
                    )
                }
            }
    ) {
        Image(
            painter = rememberAsyncImagePainter(imagePath),
            contentDescription = stringResource(Res.string.image_to_crop),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.rotationZ = rotation
                    this.scaleX = scale * if (isFlippedHorizontal) -1f else 1f
                    this.scaleY = scale * if (isFlippedVertical) -1f else 1f
                    this.translationX = offsetX
                    this.translationY = offsetY
                },
            contentScale = ContentScale.Fit
        )

        // Crop overlay - using 4 rectangles instead of BlendMode.Clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val cropSize = size.minDimension * 0.8f
                    val cropLeft = (size.width - cropSize) / 2
                    val cropTop = (size.height - cropSize) / 2
                    val cropRight = cropLeft + cropSize
                    val cropBottom = cropTop + cropSize

                    // Top overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, cropTop)
                    )
                    // Bottom overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = Offset(0f, cropBottom),
                        size = Size(size.width, size.height - cropBottom)
                    )
                    // Left overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = Offset(0f, cropTop),
                        size = Size(cropLeft, cropSize)
                    )
                    // Right overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = Offset(cropRight, cropTop),
                        size = Size(size.width - cropRight, cropSize)
                    )

                    // Draw crop border
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cropLeft, cropTop),
                        size = Size(cropSize, cropSize),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw grid lines
                    val thirdWidth = cropSize / 3
                    val thirdHeight = cropSize / 3

                    repeat(2) { i ->
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropLeft + (i + 1) * thirdWidth, cropTop),
                            end = Offset(cropLeft + (i + 1) * thirdWidth, cropBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropLeft, cropTop + (i + 1) * thirdHeight),
                            end = Offset(cropRight, cropTop + (i + 1) * thirdHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
        )
    }
}

// MARK: - Previews

@Preview
@Composable
private fun CropScreenPreview() {
    MaterialTheme {
        CropScreen(
            state = CropState(
                imagePath = "",
                scale = 1.2f
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun CropScreenProcessingPreview() {
    MaterialTheme {
        CropScreen(
            state = CropState(
                imagePath = "",
                isProcessing = true
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}
