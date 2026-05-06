package data.util

expect suspend fun applyCropTransformation(
    sourcePath: String,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    outputSize: Int = 512
): String