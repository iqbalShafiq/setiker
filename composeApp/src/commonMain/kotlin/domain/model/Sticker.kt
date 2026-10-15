package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Sticker(
    val imageFile: String,
    val sourceImageFile: String? = null,
    val emojis: List<String> = emptyList(),
    val accessibilityText: String? = null,
    val decorations: List<StickerDecoration> = emptyList(),
    val isAnimated: Boolean = false,
    val sourceVideoFile: String? = null,
    val frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap(),
    val cloudId: String? = null
) {
    companion object {
        const val MAX_EMOJIS = 3
        const val MAX_ACCESSIBILITY_TEXT_LENGTH = 125
        const val MAX_ANIMATED_ACCESSIBILITY_TEXT_LENGTH = 255
    }
}
