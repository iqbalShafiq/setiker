package domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TextDecorationStyle {
    ClassicOutline,
    StickerPop,
    BubbleRed,
    NeonCyan,
    Minimal,
    SunsetGradient,
    ApiCaption,
    Custom
}

@Serializable
data class TextEffectLayer(
    val colorArgb: Long,
    val strokeWidthRatio: Float? = null,
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = 0f,
    val shadowBlurRatio: Float = 0f,
    val shadowOffsetYRatio: Float = 0f,
    val useFillColor: Boolean = false,
    val gradientStartArgb: Long? = null,
    val gradientEndArgb: Long? = null
)

data class TextDecorationStylePreset(
    val style: TextDecorationStyle,
    val font: DecorationFont,
    val fontWeight: DecorationFontWeight,
    val defaultTextColorArgb: Long,
    val layers: List<TextEffectLayer>
)
