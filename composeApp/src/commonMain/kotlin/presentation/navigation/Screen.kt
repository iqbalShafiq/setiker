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

    @Serializable
    data class VideoTrim(val videoPath: String) : Screen()

    @Serializable
    data class VideoStickerPack(val videoPath: String) : Screen()

    @Serializable
    data class VideoCrop(
        val videoPath: String,
        val packId: String,
        val trimStartMs: Long,
        val trimEndMs: Long,
        val fps: Int,
        val speed: Float
    ) : Screen()

    @Serializable
    data class AnimatedEditor(val draftId: String, val packId: String) : Screen()

    @Serializable
    data object Login : Screen()

    @Serializable
    data object Register : Screen()

    @Serializable
    data object Profile : Screen()

    @Serializable
    data object Sync : Screen()
}
