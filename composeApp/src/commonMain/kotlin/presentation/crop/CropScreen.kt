package presentation.crop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    state: CropState,
    onIntent: (CropIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.imagePath) {
        if (state.imagePath.isNotBlank()) {
            onIntent(CropIntent.LoadImage(state.imagePath))
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Crop Image",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .padding(16.dp)
            ) {
                // Crop Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                            text = "No image selected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Zoom Slider
                Text(
                    text = "Zoom",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
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
                    CropActionButton(
                        icon = Icons.Default.Refresh,
                        label = "Rotate L",
                        onClick = { onIntent(CropIntent.RotateLeft) }
                    )
                    CropActionButton(
                        icon = Icons.Default.Refresh,
                        label = "Rotate R",
                        onClick = { onIntent(CropIntent.RotateRight) }
                    )
                    CropActionButton(
                        icon = Icons.Default.Refresh,
                        label = "Flip H",
                        onClick = { onIntent(CropIntent.FlipHorizontal) }
                    )
                    CropActionButton(
                        icon = Icons.Default.Refresh,
                        label = "Reset",
                        onClick = { onIntent(CropIntent.Reset) }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Apply Button
                AppPrimaryButton(
                    text = "Apply Crop",
                    onClick = { onIntent(CropIntent.ApplyCrop) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AppSecondaryButton(
                    text = "Cancel",
                    onClick = onBackClick
                )
            }
        }
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(
                        scale * zoom,
                        offsetX + pan.x,
                        offsetY + pan.y
                    )
                }
            }
    ) {
        Image(
            painter = rememberAsyncImagePainter(imagePath),
            contentDescription = "Image to crop",
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
        
        // Crop overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    
                    val cropSize = size.minDimension * 0.8f
                    val cropRect = Rect(
                        Offset(
                            (size.width - cropSize) / 2,
                            (size.height - cropSize) / 2
                        ),
                        Size(cropSize, cropSize)
                    )
                    
                    // Draw semi-transparent overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        size = size
                    )
                    
                    // Draw crop area (transparent)
                    drawRect(
                        color = Color.Transparent,
                        topLeft = cropRect.topLeft,
                        size = cropRect.size,
                        blendMode = BlendMode.Clear
                    )
                    
                    // Draw crop border
                    drawRect(
                        color = Color.White,
                        topLeft = cropRect.topLeft,
                        size = cropRect.size,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Draw grid lines
                    val thirdWidth = cropRect.width / 3
                    val thirdHeight = cropRect.height / 3
                    
                    repeat(2) { i ->
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropRect.left + (i + 1) * thirdWidth, cropRect.top),
                            end = Offset(cropRect.left + (i + 1) * thirdWidth, cropRect.bottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropRect.left, cropRect.top + (i + 1) * thirdHeight),
                            end = Offset(cropRect.right, cropRect.top + (i + 1) * thirdHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
        )
    }
}

@Composable
private fun CropActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
