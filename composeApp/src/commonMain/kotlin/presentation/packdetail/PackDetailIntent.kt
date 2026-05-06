package presentation.packdetail

sealed interface PackDetailIntent {
    data class LoadPack(val packId: String) : PackDetailIntent
    data class AddToWhatsApp(val packId: String) : PackDetailIntent
    data class DeletePack(val packId: String) : PackDetailIntent
    data object EditPack : PackDetailIntent
    data class DeleteSticker(val index: Int) : PackDetailIntent
    data object AddSticker : PackDetailIntent
    data class AddMultipleStickers(val imagePaths: List<String>) : PackDetailIntent
    data class EditSticker(val index: Int) : PackDetailIntent
    data class SharePack(val packId: String) : PackDetailIntent
}
