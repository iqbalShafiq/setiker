package presentation.videocrop

import presentation.common.UiText

sealed interface VideoCropEffect {
    data class NavigateToAnimatedEditor(val draftId: String) : VideoCropEffect
    data object NavigateBack : VideoCropEffect
    data class ShowError(val message: UiText) : VideoCropEffect
}
