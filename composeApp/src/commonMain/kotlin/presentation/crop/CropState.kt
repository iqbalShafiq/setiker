package presentation.crop

data class CropState(
    val imagePath: String = "",
    val isProcessing: Boolean = false,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    /** Offset normalized to the square preview/output box. -1f means one full box width left. */
    val offsetX: Float = 0f,
    /** Offset normalized to the square preview/output box. -1f means one full box height up. */
    val offsetY: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val croppedImagePath: String? = null,
    val error: String? = null
)
