package presentation.videotrim

import domain.model.AnimatedStickerSpec
import presentation.common.UiText

sealed interface VideoTrimEffect {
    data class NavigateToVideoCrop(
        val videoPath: String,
        val spec: AnimatedStickerSpec
    ) : VideoTrimEffect

    data object NavigateBack : VideoTrimEffect
    data class ShowError(val message: UiText) : VideoTrimEffect
}
