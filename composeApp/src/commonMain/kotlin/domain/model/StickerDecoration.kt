package domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class DecorationFont {
    Sans,
    Serif,
    Mono,
    Cursive,
    Display,
    Rounded,
    Condensed
}

@Serializable
enum class DecorationFontWeight {
    Light,
    Regular,
    Medium,
    SemiBold,
    Bold
}

@Serializable
enum class TextDecorationSource {
    User,
    ApiTextAsset,
    ApiOutsideForeground
}

@Serializable
enum class TextDecorationLayout {
    Freeform,
    BottomCaption
}

@Serializable
sealed interface StickerDecoration {
    val id: String
    val centerX: Float
    val centerY: Float
    val scale: Float
}

@Serializable
@SerialName("text")
data class TextDecoration(
    override val id: String,
    val text: String,
    val font: DecorationFont,
    val fontWeight: DecorationFontWeight = DecorationFontWeight.Regular,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val source: TextDecorationSource = TextDecorationSource.User,
    val layout: TextDecorationLayout = TextDecorationLayout.Freeform,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration

fun StickerDecoration.normalizedForCurrentSchema(): StickerDecoration = when (this) {
    is TextDecoration -> if (layout == TextDecorationLayout.Freeform && id.startsWith("api_txt_")) {
        copy(
            source = TextDecorationSource.ApiOutsideForeground,
            layout = TextDecorationLayout.BottomCaption
        )
    } else {
        this
    }
    else -> this
}

fun decodeStickerDecorationsForCurrentSchema(raw: String?): List<StickerDecoration> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        Json.decodeFromString<List<StickerDecoration>>(raw)
            .map { it.normalizedForCurrentSchema() }
    } catch (_: Exception) {
        emptyList()
    }
}

@Serializable
@SerialName("emoji")
data class EmojiDecoration(
    override val id: String,
    val emoji: String,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration

@Serializable
@SerialName("image")
data class ImageDecoration(
    override val id: String,
    val imagePath: String,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration
