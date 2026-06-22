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
    Condensed,
    Bungee,
    LuckiestGuy,
    Fredoka
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
    BottomCaption,
    Arched
}

@Serializable
sealed interface StickerDecoration {
    val id: String
    val centerX: Float
    val centerY: Float
    val scale: Float
}

const val DEFAULT_DECORATION_BORDER_COLOR_ARGB: Long = 0xFFFFFFFFL
const val DEFAULT_DECORATION_BORDER_WIDTH_RATIO: Float = 0.08f

@Serializable
@SerialName("text")
data class TextDecoration(
    override val id: String,
    val text: String,
    val font: DecorationFont,
    val fontWeight: DecorationFontWeight = DecorationFontWeight.Regular,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val borderColorArgb: Long = DEFAULT_DECORATION_BORDER_COLOR_ARGB,
    val borderWidthRatio: Float = DEFAULT_DECORATION_BORDER_WIDTH_RATIO,
    val style: TextDecorationStyle = TextDecorationStyle.ClassicOutline,
    val source: TextDecorationSource = TextDecorationSource.User,
    val layout: TextDecorationLayout = TextDecorationLayout.Freeform,
    /** Arc intensity for [TextDecorationLayout.Arched], in range -1..1. */
    val arcIntensity: Float = 0.35f,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration

fun StickerDecoration.normalizedForCurrentSchema(): StickerDecoration = when (this) {
    is TextDecoration -> {
        val migrated = if (layout == TextDecorationLayout.Freeform && id.startsWith("api_txt_")) {
            copy(
                source = TextDecorationSource.ApiOutsideForeground,
                layout = TextDecorationLayout.BottomCaption
            )
        } else {
            this
        }
        if (migrated.style == TextDecorationStyle.ClassicOutline) {
            migrated.copy(style = migrated.inferStyleFromLegacyFields())
        } else {
            migrated
        }
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
    val borderColorArgb: Long = DEFAULT_DECORATION_BORDER_COLOR_ARGB,
    val borderWidthRatio: Float = DEFAULT_DECORATION_BORDER_WIDTH_RATIO,
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
