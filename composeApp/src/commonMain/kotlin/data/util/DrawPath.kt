package data.util

data class DrawPath(
    val points: List<Pair<Float, Float>>,
    val brushSize: Float,
    val isErasing: Boolean
)
