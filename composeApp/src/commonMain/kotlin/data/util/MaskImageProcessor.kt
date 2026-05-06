package data.util

import androidx.compose.ui.unit.IntSize
import presentation.backgroundremover.DrawPath

expect suspend fun applyMaskToImage(
    imagePath: String,
    paths: List<DrawPath>,
    canvasSize: IntSize
): String