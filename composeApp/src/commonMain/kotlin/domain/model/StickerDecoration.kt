package domain.model

enum class DecorationFont {
    Sans,
    Serif,
    Mono,
    Cursive,
    Display,
    Rounded,
    Condensed
}

enum class DecorationFontWeight {
    Light,
    Regular,
    Medium,
    SemiBold,
    Bold
}

sealed interface StickerDecoration {
    val id: String
    val centerX: Float
    val centerY: Float
    val scale: Float
}

data class TextDecoration(
    override val id: String,
    val text: String,
    val font: DecorationFont,
    val fontWeight: DecorationFontWeight = DecorationFontWeight.Regular,
    val textColorArgb: Long = 0xFFFFFFFFL,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration

data class EmojiDecoration(
    override val id: String,
    val emoji: String,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration

data class ImageDecoration(
    override val id: String,
    val imagePath: String,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration
