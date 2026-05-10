package presentation.videocrop

import domain.model.AnimatedStickerSpec

sealed interface VideoCropIntent {
    data class Load(
        val videoPath: String,
        val spec: AnimatedStickerSpec,
        val packId: String
    ) : VideoCropIntent

    data class UpdateScale(val scale: Float) : VideoCropIntent
    data class UpdateOffset(val xNorm: Float, val yNorm: Float) : VideoCropIntent
    data object RotateLeft : VideoCropIntent
    data object RotateRight : VideoCropIntent
    data object FlipHorizontal : VideoCropIntent
    data object Reset : VideoCropIntent
    data object PlayPreview : VideoCropIntent
    data object PausePreview : VideoCropIntent
    data object Apply : VideoCropIntent
    data object Cancel : VideoCropIntent
}
