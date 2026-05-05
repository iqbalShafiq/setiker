package presentation.crop

sealed interface CropIntent {
    data class LoadImage(val path: String) : CropIntent
    data object RotateLeft : CropIntent
    data object RotateRight : CropIntent
    data object FlipHorizontal : CropIntent
    data object FlipVertical : CropIntent
    data class UpdateScale(val scale: Float) : CropIntent
    data class UpdateOffset(val x: Float, val y: Float) : CropIntent
    data object ApplyCrop : CropIntent
    data object Reset : CropIntent
}
