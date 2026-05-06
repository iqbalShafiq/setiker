package presentation.packdetail

sealed interface PackDetailEffect {
    data object NavigateBack : PackDetailEffect
    data object NavigateToEditPack : PackDetailEffect
    data object NavigateToAddSticker : PackDetailEffect
    data class NavigateToEditSticker(val index: Int) : PackDetailEffect
    data class ShowError(val message: String) : PackDetailEffect
    data class ShowSuccess(val message: String) : PackDetailEffect
    data class ShowShareSheet(val packId: String) : PackDetailEffect
}
