package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import org.jetbrains.compose.resources.Font
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.bungee_regular
import setiker.composeapp.generated.resources.fredoka_bold
import setiker.composeapp.generated.resources.luckiest_guy_regular

@Composable
fun decorationFontFamily(
    font: DecorationFont,
    weight: DecorationFontWeight = DecorationFontWeight.Regular
): FontFamily = when (font) {
    DecorationFont.Bungee -> FontFamily(Font(Res.font.bungee_regular))
    DecorationFont.LuckiestGuy -> FontFamily(Font(Res.font.luckiest_guy_regular))
    DecorationFont.Fredoka -> FontFamily(Font(Res.font.fredoka_bold, mapDecorationFontWeight(weight)))
    DecorationFont.Sans -> FontFamily.SansSerif
    DecorationFont.Serif -> FontFamily.Serif
    DecorationFont.Mono -> FontFamily.Monospace
    DecorationFont.Cursive -> FontFamily.Cursive
    DecorationFont.Display -> FontFamily.Serif
    DecorationFont.Rounded -> FontFamily.SansSerif
    DecorationFont.Condensed -> FontFamily.SansSerif
}

fun mapDecorationFontWeight(weight: DecorationFontWeight): FontWeight = when (weight) {
    DecorationFontWeight.Light -> FontWeight.Light
    DecorationFontWeight.Regular -> FontWeight.Normal
    DecorationFontWeight.Medium -> FontWeight.Medium
    DecorationFontWeight.SemiBold -> FontWeight.SemiBold
    DecorationFontWeight.Bold -> FontWeight.Bold
}
