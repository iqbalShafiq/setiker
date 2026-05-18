package presentation.videostickerpack

import presentation.common.UiText

sealed interface VideoStickerPackEffect {
    data class NavigateToPackDetail(val packId: String) : VideoStickerPackEffect
    data object NavigateBack : VideoStickerPackEffect
    data class ShowError(val message: UiText) : VideoStickerPackEffect
}
