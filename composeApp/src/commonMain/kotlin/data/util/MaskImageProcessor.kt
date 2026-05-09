package data.util

import androidx.compose.ui.unit.IntSize

expect suspend fun applyMaskToImage(
    imagePath: String,
    paths: List<DrawPath>,
    canvasSize: IntSize
): String