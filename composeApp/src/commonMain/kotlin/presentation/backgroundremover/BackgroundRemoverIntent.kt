package presentation.backgroundremover

sealed interface BackgroundRemoverIntent {
    data class LoadImage(val path: String) : BackgroundRemoverIntent
    data class UpdateBrushSize(val size: Float) : BackgroundRemoverIntent
    data object ToggleMode : BackgroundRemoverIntent
    data class AddPath(val path: DrawPath) : BackgroundRemoverIntent
    data object Undo : BackgroundRemoverIntent
    data object ClearAll : BackgroundRemoverIntent
    data object AutoRemove : BackgroundRemoverIntent
    data object ApplyRemoval : BackgroundRemoverIntent
    data object Reset : BackgroundRemoverIntent
}
