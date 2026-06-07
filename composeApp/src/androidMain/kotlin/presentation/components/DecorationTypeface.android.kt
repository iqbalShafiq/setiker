package presentation.components

import android.content.Context
import android.graphics.Typeface
import domain.model.DecorationFont
import domain.model.DecorationFontWeight

private val typefaceCache = mutableMapOf<String, Typeface>()

fun decorationTypeface(
    context: Context,
    font: DecorationFont,
    weight: DecorationFontWeight
): Typeface {
    val cacheKey = "${font.name}_${weight.name}"
    return typefaceCache.getOrPut(cacheKey) {
        loadDecorationTypeface(context, font, weight)
    }
}

private fun loadDecorationTypeface(
    context: Context,
    font: DecorationFont,
    weight: DecorationFontWeight
): Typeface {
    val assetName = when (font) {
        DecorationFont.Bungee -> "fonts/bungee_regular.ttf"
        DecorationFont.LuckiestGuy -> "fonts/luckiest_guy_regular.ttf"
        DecorationFont.Fredoka -> "fonts/fredoka_bold.ttf"
        else -> null
    }
    if (assetName != null) {
        try {
            return Typeface.createFromAsset(context.assets, assetName)
        } catch (_: Exception) {
            // fall through to system typeface
        }
    }
    return systemTypeface(font, weight)
}

private fun systemTypeface(font: DecorationFont, weight: DecorationFontWeight): Typeface {
    val base = when (font) {
        DecorationFont.Sans -> Typeface.SANS_SERIF
        DecorationFont.Serif -> Typeface.SERIF
        DecorationFont.Mono -> Typeface.MONOSPACE
        DecorationFont.Cursive -> Typeface.create("cursive", Typeface.NORMAL)
        DecorationFont.Display -> Typeface.create("serif", Typeface.NORMAL)
        DecorationFont.Rounded -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        DecorationFont.Condensed -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        DecorationFont.Bungee,
        DecorationFont.LuckiestGuy,
        DecorationFont.Fredoka -> Typeface.SANS_SERIF
    }
    return when (weight) {
        DecorationFontWeight.SemiBold,
        DecorationFontWeight.Bold -> Typeface.create(base, Typeface.BOLD)
        else -> base
    }
}
