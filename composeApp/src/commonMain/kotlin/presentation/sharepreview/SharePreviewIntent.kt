package presentation.sharepreview

sealed interface SharePreviewIntent {
    data class Load(val kind: String, val token: String) : SharePreviewIntent
    data object Accept : SharePreviewIntent
    data object NavigateBack : SharePreviewIntent
}
