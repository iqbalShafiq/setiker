package presentation.videotrim

sealed interface VideoTrimIntent {
    data class LoadVideo(val videoPath: String) : VideoTrimIntent
    data class UpdateTrimStart(val ms: Long) : VideoTrimIntent
    data class UpdateTrimEnd(val ms: Long) : VideoTrimIntent
    data class UpdateFps(val fps: Int) : VideoTrimIntent
    data class UpdateSpeed(val speed: Float) : VideoTrimIntent
    data object PlayPreview : VideoTrimIntent
    data object PausePreview : VideoTrimIntent
    data object Confirm : VideoTrimIntent
    data object Cancel : VideoTrimIntent
}
