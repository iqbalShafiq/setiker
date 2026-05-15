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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalDialogRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
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
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        if (state.isProcessing) {
            LoadingIndicator(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            val border = neubrutalBorderColor()
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
                            offsetX = NeubrutalShadowOffset,
                            offsetY = NeubrutalShadowOffset,
                            cornerRadius = NeubrutalDialogRadius,
                            color = neubrutalShadowColor()
                        )
                        .clip(RoundedCornerShape(NeubrutalDialogRadius))
                        .background(neubrutalCardSurface())
                        .border(
                            width = NeubrutalBorderWidth,
                            color = border,
                            shape = RoundedCornerShape(NeubrutalDialogRadius)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imagePath.isNotBlank()) {
                        CropImagePreview(
                            imagePath = state.imagePath,
                            rotation = state.rotation,
                            scale = state.scale,
                            offsetXNorm = state.offsetX,
                            offsetYNorm = state.offsetY,
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
                            color = neubrutalSubtleOnSurface()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Zoom Slider
                Text(
                    text = stringResource(Res.string.zoom),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = neubrutalOnSurface()
                )

                Slider(
                    value = state.scale,
                    onValueChange = { onIntent(CropIntent.UpdateScale(it)) },
                    valueRange = 0.5f..4f,
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
    val border = neubrutalBorderColor()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .neubrutalShadow(
                    offsetX = NeubrutalSmallShadowOffset,
                    offsetY = NeubrutalSmallShadowOffset,
                    cornerRadius = 24.dp,
                    color = neubrutalShadowColor()
                )
                .clip(CircleShape)
                .background(if (isSelected) AccentCoral else neubrutalCardSurface())
                .border(
                    width = NeubrutalBorderWidth,
                    color = border,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) NeubrutalWhite else neubrutalOnSurface()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = neubrutalMutedOnSurface()
        )
    }
}

@Composable
private fun CropImagePreview(
    imagePath: String,
    rotation: Float,
    scale: Float,
    offsetXNorm: Float,
    offsetYNorm: Float,
    isFlippedHorizontal: Boolean,
    isFlippedVertical: Boolean,
    onTransform: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use rememberUpdatedState to always have latest values in gesture handler
    val currentScale by rememberUpdatedState(scale)
    val currentOffsetX by rememberUpdatedState(offsetXNorm)
    val currentOffsetY by rememberUpdatedState(offsetYNorm)
    val currentOnTransform by rememberUpdatedState(onTransform)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val boxW = size.width.toFloat().coerceAtLeast(1f)
                    val boxH = size.height.toFloat().coerceAtLeast(1f)
                    currentOnTransform(
                        currentScale * zoom,
                        currentOffsetX + pan.x / boxW,
                        currentOffsetY + pan.y / boxH
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
                    this.translationX = offsetXNorm * size.width
                    this.translationY = offsetYNorm * size.height
                },
            contentScale = ContentScale.Fit
        )

        // Rule-of-thirds grid over the exact square that will be exported.
        // Earlier this screen drew an 80% inner crop box, but the Android crop
        // processor exported the full square. That visual-only box made the
        // preview feel different from the result. Now the visible square frame
        // itself is the crop area, matching VideoCropScreen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val thirdWidth = size.width / 3
                    val thirdHeight = size.height / 3

                    repeat(2) { i ->
                        drawLine(
                            color = NeubrutalWhite.copy(alpha = 0.6f),
                            start = Offset((i + 1) * thirdWidth, 0f),
                            end = Offset((i + 1) * thirdWidth, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = NeubrutalWhite.copy(alpha = 0.6f),
                            start = Offset(0f, (i + 1) * thirdHeight),
                            end = Offset(size.width, (i + 1) * thirdHeight),
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
