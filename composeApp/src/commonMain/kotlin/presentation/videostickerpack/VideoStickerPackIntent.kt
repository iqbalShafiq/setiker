package presentation.videostickerpack

sealed interface VideoStickerPackIntent {
    data class LoadVideo(val path: String) : VideoStickerPackIntent
    data class UpdateStart(val ms: Long) : VideoStickerPackIntent
    data class UpdateEnd(val ms: Long) : VideoStickerPackIntent
    data class ScrubPreviewTo(val index: Int) : VideoStickerPackIntent
    data class UpdatePrompt(val value: String) : VideoStickerPackIntent
    data class UpdatePackName(val value: String) : VideoStickerPackIntent
    data class UpdatePublisher(val value: String) : VideoStickerPackIntent
    data class ToggleStaticStickerSelection(val key: String) : VideoStickerPackIntent
    data class ToggleAnimatedStickerSelection(val key: String) : VideoStickerPackIntent
    data object PlayPreview : VideoStickerPackIntent
    data object PausePreview : VideoStickerPackIntent
    data object Generate : VideoStickerPackIntent
    data object Regenerate : VideoStickerPackIntent
    data object SavePack : VideoStickerPackIntent
}
