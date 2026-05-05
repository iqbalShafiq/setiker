package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Sticker(
    val imageFile: String,
    val emojis: List<String> = emptyList(),
    val accessibilityText: String? = null
) {
    companion object {
        const val MAX_EMOJIS = 3
        const val MAX_ACCESSIBILITY_TEXT_LENGTH = 125
    }
}
