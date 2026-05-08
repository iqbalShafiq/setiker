package presentation.crop

import presentation.common.UiText

sealed interface CropEffect {
    data class ImageCropped(val path: String) : CropEffect
    data object NavigateBack : CropEffect
    data class ShowError(val message: UiText) : CropEffect
}
