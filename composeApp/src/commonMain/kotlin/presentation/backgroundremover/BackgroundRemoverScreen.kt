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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource
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
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.apply_remove_background
import setiker.composeapp.generated.resources.brush_size_px
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.clear
import setiker.composeapp.generated.resources.erase
import setiker.composeapp.generated.resources.image_to_edit
import setiker.composeapp.generated.resources.no_image_selected
import setiker.composeapp.generated.resources.remove_background_title
import setiker.composeapp.generated.resources.remove_bg_canvas_hint
import setiker.composeapp.generated.resources.remove_bg_preview_content_description
import setiker.composeapp.generated.resources.remove_bg_result_hint
import setiker.composeapp.generated.resources.reset
import setiker.composeapp.generated.resources.restore
import setiker.composeapp.generated.resources.result_confirmation_title
import setiker.composeapp.generated.resources.undo
import setiker.composeapp.generated.resources.use_result

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
    val resultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (state.isResultSheetOpen && !state.removedBackgroundPath.isNullOrBlank()) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(BackgroundRemoverIntent.DismissResultSheet) },
            sheetState = resultSheetState,
            containerColor = NeubrutalBg,
            scrimColor = NeubrutalBlack.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.result_confirmation_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeubrutalBlack
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.remove_bg_result_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalBlack.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeubrutalWhite)
                        .border(2.dp, NeubrutalBlack, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                    Image(
                        painter = rememberAsyncImagePainter(state.removedBackgroundPath),
                        contentDescription = stringResource(Res.string.remove_bg_preview_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                AppPrimaryButton(
                    text = stringResource(Res.string.use_result),
                    onClick = { onIntent(BackgroundRemoverIntent.ConfirmResult) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(BackgroundRemoverIntent.DismissResultSheet) }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.remove_background_title),
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
                        .padding(4.dp)
                        .onGloballyPositioned { coordinates ->
                            val size = coordinates.size.toSize()
                            if (size.width > 0 && size.height > 0) {
                                android.util.Log.d(
                                    "BackgroundRemover",
                                    "Canvas size: ${size.width}x${size.height}"
                                )
                                onIntent(
                                    BackgroundRemoverIntent.UpdateCanvasSize(
                                        size.width.toInt(),
                                        size.height.toInt()
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imagePath.isNotBlank()) {
                        // Checkerboard background to show transparency
                        CheckerboardBackground(
                            modifier = Modifier.fillMaxSize()
                        )

                        Image(
                            painter = rememberAsyncImagePainter(state.imagePath),
                            contentDescription = stringResource(Res.string.image_to_edit),
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isDrawing = true
                                            currentPath = listOf(it.x to it.y)
                                            android.util.Log.d(
                                                "BackgroundRemover",
                                                "Drag start: ${it.x}, ${it.y}"
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            if (isDrawing) {
                                                currentPath = currentPath + (change.position.x to change.position.y)
                                            }
                                        },
                                        onDragEnd = {
                                            isDrawing = false
                                            if (currentPath.isNotEmpty()) {
                                                android.util.Log.d(
                                                    "BackgroundRemover",
                                                    "Drag end: ${currentPath.size} points"
                                                )
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
                            text = stringResource(Res.string.no_image_selected),
                            style = MaterialTheme.typography.bodyLarge,
                            color = NeubrutalGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.remove_bg_canvas_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalBlack.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Brush Size
                Text(
                    text = stringResource(Res.string.brush_size_px, state.brushSize.toInt()),
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
                        label = stringResource(if (state.isErasing) Res.string.erase else Res.string.restore),
                        isSelected = state.isErasing,
                        onClick = { onIntent(BackgroundRemoverIntent.ToggleMode) }
                    )
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = stringResource(Res.string.undo),
                        isSelected = false,
                        onClick = { onIntent(BackgroundRemoverIntent.Undo) }
                    )
                    NeubrutalToolButton(
                        icon = Icons.Default.Refresh,
                        label = stringResource(Res.string.clear),
                        isSelected = false,
                        onClick = { onIntent(BackgroundRemoverIntent.ClearAll) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                AppPrimaryButton(
                    text = stringResource(Res.string.apply_remove_background),
                    enabled = state.imagePath.isNotBlank(),
                    onClick = {
                        android.util.Log.d("BackgroundRemover", "Apply clicked with ${state.paths.size} paths")
                        onIntent(BackgroundRemoverIntent.ApplyRemoval)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppSecondaryButton(
                    text = stringResource(Res.string.reset),
                    onClick = { onIntent(BackgroundRemoverIntent.Reset) }
                )
            }
        }
    }
}

@Composable
private fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    checkerColor: Color = Color.LightGray,
    squareSize: Float = 20f
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .drawBehind {
                val numSquaresX = (size.width / squareSize).toInt() + 1
                val numSquaresY = (size.height / squareSize).toInt() + 1

                for (x in 0 until numSquaresX) {
                    for (y in 0 until numSquaresY) {
                        if ((x + y) % 2 == 0) {
                            drawRect(
                                color = checkerColor,
                                topLeft = Offset(x * squareSize, y * squareSize),
                                size = Size(squareSize, squareSize)
                            )
                        }
                    }
                }
            }
    )
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
