package presentation.createpack

sealed interface CreatePackIntent {
    data class UpdateName(val name: String) : CreatePackIntent
    data class UpdatePublisher(val publisher: String) : CreatePackIntent
    data class UpdateTrayImage(val imagePath: String) : CreatePackIntent
    data class AddSticker(val imagePath: String) : CreatePackIntent
    data class RemoveSticker(val index: Int) : CreatePackIntent
    data object SavePack : CreatePackIntent
    data class LoadPack(val packId: String) : CreatePackIntent
}
