package data.remote.mapper

import data.remote.model.ApiImage
import data.remote.model.ApiTextAsset
import data.remote.model.ApiTextAssetDecoration
import data.remote.model.ApiTextOutsideForeground
import data.remote.model.ApiTextOutsideForegroundStyle
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.TextDecorationLayout
import domain.model.TextDecorationSource

private const val DEFAULT_BOTTOM_CENTER_Y = 0.88f
private const val DEFAULT_OVERLAY_SCALE = 0.58f

fun ApiImage.toStickerDecorations(): List<StickerDecoration> = listOfNotNull(
    textAssetDecoration.toTextDecoration(
        id = "api_text_${TextDecorationSource.ApiTextAsset.name}_$id",
        source = TextDecorationSource.ApiTextAsset
    ) ?: textOutsideForeground.toTextDecoration(
        id = "api_text_${TextDecorationSource.ApiOutsideForeground.name}_$id",
        source = TextDecorationSource.ApiOutsideForeground
    )
)

internal fun ApiTextAsset.toDecorationApiImage(): ApiImage = ApiImage(
    id = id,
    url = "",
    textAssetDecoration = textAssetDecoration,
    textOutsideForeground = textOutsideForeground
)

private fun ApiTextAssetDecoration?.toTextDecoration(
    id: String,
    source: TextDecorationSource
): TextDecoration? {
    val raw = this ?: return null
    return buildApiTextDecoration(id = id, text = raw.text, style = raw.style, source = source)
}

private fun ApiTextOutsideForeground?.toTextDecoration(
    id: String,
    source: TextDecorationSource
): TextDecoration? {
    val raw = this ?: return null
    return buildApiTextDecoration(id = id, text = raw.text, style = raw.style, source = source)
}

private fun buildApiTextDecoration(
    id: String,
    text: String?,
    style: ApiTextOutsideForegroundStyle?,
    source: TextDecorationSource
): TextDecoration? {
    val trimmed = text?.trim().orEmpty()
    if (trimmed.isEmpty()) return null

    return TextDecoration(
        id = id,
        text = trimmed,
        font = mapApiFontFamily(style?.fontFamily),
        fontWeight = mapApiFontWeight(style?.weight),
        textColorArgb = parseApiColorToArgb(style?.color) ?: 0xFF000000L,
        source = source,
        layout = TextDecorationLayout.BottomCaption,
        centerX = 0.5f,
        centerY = DEFAULT_BOTTOM_CENTER_Y,
        scale = DEFAULT_OVERLAY_SCALE
    )
}

internal fun parseApiColorToArgb(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim()
    return when {
        s.startsWith("#") -> parseHexColorString(s.removePrefix("#").trim())
        s.startsWith("rgb(", ignoreCase = true) ||
            s.startsWith("rgba(", ignoreCase = true) -> parseRgbFunction(s)
        else -> parseNamedCssColor(s)
    }
}

private fun parseHexColorString(hex: String): Long? {
    if (hex.isEmpty()) return null
    return try {
        when (hex.length) {
            3 -> {
                val r = "${hex[0]}${hex[0]}".toLong(16)
                val g = "${hex[1]}${hex[1]}".toLong(16)
                val b = "${hex[2]}${hex[2]}".toLong(16)
                (0xFF000000L) or (r shl 16) or (g shl 8) or b
            }
            6 -> 0xFF000000L or hex.toLong(16)
            8 -> {
                val r = hex.substring(0, 2).toLong(16)
                val g = hex.substring(2, 4).toLong(16)
                val b = hex.substring(4, 6).toLong(16)
                val a = hex.substring(6, 8).toLong(16)
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseRgbFunction(s: String): Long? {
    val inner = s.removePrefix("rgb(").removePrefix("RGB(").removePrefix("rgba(").removePrefix("RGBA(")
        .removeSuffix(")").trim()
    val parts = inner.split(',').map { it.trim() }
    if (parts.size < 3) return null
    return try {
        val r = parseRgbChannel(parts[0])
        val g = parseRgbChannel(parts[1])
        val b = parseRgbChannel(parts[2])
        val a = if (parts.size >= 4) {
            val raw = parts[3]
            when {
                raw.endsWith("%") -> (raw.removeSuffix("%").toFloat() / 100f * 255f).toInt()
                else -> (raw.toFloat().coerceIn(0f, 1f) * 255f).toInt()
            }.coerceIn(0, 255).toLong()
        } else {
            255L
        }
        (a shl 24) or (r shl 16) or (g shl 8) or b
    } catch (_: Exception) {
        null
    }
}

private fun parseRgbChannel(part: String): Long {
    val p = part.trim()
    return if (p.endsWith("%")) {
        (p.removeSuffix("%").toFloat() / 100f * 255f).toInt()
    } else {
        p.toFloat().toInt()
    }.coerceIn(0, 255).toLong()
}

private fun parseNamedCssColor(name: String): Long? {
    return when (name.lowercase()) {
        "black" -> 0xFF000000L
        "white" -> 0xFFFFFFFFL
        "red" -> 0xFFFF0000L
        "green" -> 0xFF00FF00L
        "blue" -> 0xFF0000FFL
        "transparent" -> 0x00000000L
        else -> null
    }
}

private fun mapApiFontFamily(raw: String?): DecorationFont {
    if (raw.isNullOrBlank()) return DecorationFont.Sans
    val s = raw.lowercase()
    return when {
        "mono" in s || "courier" in s || "consolas" in s -> DecorationFont.Mono
        "serif" in s && "sans" !in s -> DecorationFont.Serif
        "cursive" in s || "script" in s -> DecorationFont.Cursive
        "rounded" in s -> DecorationFont.Rounded
        "condensed" in s -> DecorationFont.Condensed
        "display" in s -> DecorationFont.Display
        else -> DecorationFont.Sans
    }
}

private fun mapApiFontWeight(raw: String?): DecorationFontWeight {
    if (raw.isNullOrBlank()) return DecorationFontWeight.Regular
    val s = raw.trim().lowercase()
    val numeric = s.filter { it.isDigit() }.toIntOrNull()
    if (numeric != null) {
        return when {
            numeric >= 700 -> DecorationFontWeight.Bold
            numeric >= 600 -> DecorationFontWeight.SemiBold
            numeric >= 500 -> DecorationFontWeight.Medium
            numeric <= 300 -> DecorationFontWeight.Light
            else -> DecorationFontWeight.Regular
        }
    }
    return when {
        "bold" in s || "heavy" in s -> DecorationFontWeight.Bold
        "semi" in s || "demi" in s -> DecorationFontWeight.SemiBold
        "medium" in s -> DecorationFontWeight.Medium
        "light" in s || "thin" in s -> DecorationFontWeight.Light
        "regular" in s || "normal" in s || "book" in s -> DecorationFontWeight.Regular
        else -> DecorationFontWeight.Regular
    }
}
