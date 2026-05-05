package presentation.crop

sealed interface CropEffect {
    data class ImageCropped(val path: String) : CropEffect
    data object NavigateBack : CropEffect
    data class ShowError(val message: String) : CropEffect
}
