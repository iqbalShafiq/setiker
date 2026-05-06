package presentation.backgroundremover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.theme.AccentCoral
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundRemoverScreen(
    state: BackgroundRemoverState,
    onIntent: (BackgroundRemoverIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var isDrawing by remember { mutableStateOf(false) }

    LaunchedEffect(state.imagePath) {
        if (state.imagePath.isNotBlank()) {
            onIntent(BackgroundRemoverIntent.LoadImage(state.imagePath))
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Remove Background",
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
                // Canvas Area (Neubrutal Frame)
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
                        Image(
                            painter = rememberAsyncImagePainter(state.imagePath),
                            contentDescription = "Image to edit",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isDrawing = true
                                            currentPath = listOf(it.x to it.y)
                                        },
                                        onDrag = { change, _ ->
                                            if (isDrawing) {
                                                currentPath = currentPath + (change.position.x to change.position.y)
                                            }
                                        },
                                        onDragEnd = {
                                            isDrawing = false
                                            if (currentPath.isNotEmpty()) {
                                                onIntent(
                                                    BackgroundRemoverIntent.AddPath(
                                                        DrawPath(
                                                            points = currentPath,
                                                            brushSize = state.brushSize,
                                                            isErasing = state.isErasing
                                                        )
                                                    )
                                                )
                                                currentPath = emptyList()
                                            }
                                        }
                                    )
                                },
                            contentScale = ContentScale.Fit
                        )

                        // Draw paths overlay
                        if (state.paths.isNotEmpty() || currentPath.isNotEmpty()) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                state.paths.forEach { path ->
                                    val strokeWidth = path.brushSize
                                    val color = if (path.isErasing) {
                                        ErrorRed.copy(alpha = 0.5f)
                                    } else {
                                        Color.Green.copy(alpha = 0.5f)
                                    }

                                    if (path.points.size > 1) {
                                        val androidPath = Path().apply {
                                            moveTo(path.points.first().first, path.points.first().second)
                                            path.points.drop(1).forEach {
                                                lineTo(it.first, it.second)
                                            }
                                        }
                                        drawPath(
                                            path = androidPath,
                                            color = color,
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                }

                                // Draw current path
                                if (currentPath.size > 1) {
                                    val strokeWidth = state.brushSize
                                    val color = if (state.isErasing) {
                                        ErrorRed.copy(alpha = 0.7f)
                                    } else {
                                        Color.Green.copy(alpha = 0.7f)
                                    }

                                    val androidPath = Path().apply {
                                        moveTo(currentPath.first().first, currentPath.first().second)
                                        currentPath.drop(1).forEach {
                                            lineTo(it.first, it.second)
                                        }
                                    }
                                    drawPath(
                                        path = androidPath,
                                        color = color,
                                        style = Stroke(width = strokeWidth)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No image selected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NeubrutalGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Brush Size
                Text(
                    text = "Brush Size: ${state.brushSize.toInt()}px",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = NeubrutalBlack
                )

                Slider(
                    value = state.brushSize,
                    onValueChange = { onIntent(BackgroundRemoverIntent.UpdateBrushSize(it)) },
                    valueRange = 5f..50f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = if (state.isErasing) "Erase" else "Restore",
                        isSelected = state.isErasing,
                        onClick = { onIntent(BackgroundRemoverIntent.ToggleMode) }
                    )
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = "Undo",
                        isSelected = false,
                        onClick = { onIntent(BackgroundRemoverIntent.Undo) }
                    )
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = "Auto",
                        isSelected = false,
                        onClick = { onIntent(BackgroundRemoverIntent.AutoRemove) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                AppPrimaryButton(
                    text = "Apply",
                    onClick = { onIntent(BackgroundRemoverIntent.ApplyRemoval) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppSecondaryButton(
                    text = "Reset",
                    onClick = { onIntent(BackgroundRemoverIntent.Reset) }
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

// MARK: - Previews

@Preview
@Composable
private fun BackgroundRemoverScreenPreview() {
    MaterialTheme {
        BackgroundRemoverScreen(
            state = BackgroundRemoverState(
                imagePath = "",
                brushSize = 20f,
                isErasing = true
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun BackgroundRemoverScreenProcessingPreview() {
    MaterialTheme {
        BackgroundRemoverScreen(
            state = BackgroundRemoverState(
                imagePath = "",
                isProcessing = true
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}
