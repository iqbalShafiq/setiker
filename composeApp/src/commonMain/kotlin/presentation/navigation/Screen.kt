package presentation.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object Home : Screen()
    
    @Serializable
    data class PackDetail(val packId: String) : Screen()
    
    @Serializable
    data class CreatePack(val packId: String? = null) : Screen()
    
    @Serializable
    data class Editor(val stickerIndex: Int? = null, val packId: String) : Screen()
    
    @Serializable
    data class Crop(val imagePath: String) : Screen()
}
