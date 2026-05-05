package presentation.backgroundremover

data class BackgroundRemoverState(
    val imagePath: String = "",
    val isProcessing: Boolean = false,
    val brushSize: Float = 20f,
    val isErasing: Boolean = true,
    val paths: List<DrawPath> = emptyList(),
    val removedBackgroundPath: String? = null,
    val error: String? = null
)

data class DrawPath(
    val points: List<Pair<Float, Float>>,
    val brushSize: Float,
    val isErasing: Boolean
)
