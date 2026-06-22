package presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.instrument_sans_bold
import setiker.composeapp.generated.resources.instrument_sans_medium
import setiker.composeapp.generated.resources.instrument_sans_regular
import setiker.composeapp.generated.resources.instrument_sans_semibold
import setiker.composeapp.generated.resources.space_grotesk_bold
import setiker.composeapp.generated.resources.space_grotesk_medium
import setiker.composeapp.generated.resources.space_grotesk_regular
import setiker.composeapp.generated.resources.space_grotesk_semibold

/* ============================================
 * Custom Neubrutal Font Families
 * ============================================ */

/** Instrument Sans — used for display, headlines, titles. */
val InstrumentSansFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.instrument_sans_regular, FontWeight.Normal),
        Font(Res.font.instrument_sans_medium, FontWeight.Medium),
        Font(Res.font.instrument_sans_semibold, FontWeight.SemiBold),
        Font(Res.font.instrument_sans_bold, FontWeight.Bold)
    )

/** Space Grotesk — used for body text, labels, buttons. */
val SpaceGroteskFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.space_grotesk_regular, FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(Res.font.space_grotesk_bold, FontWeight.Bold)
    )

/* ============================================
 * Typography
 * ============================================
 *
 * Instrument Sans for headlines / titles (strong presence)
 * Space Grotesk for body / labels (readable, playful)
 */

@Composable
fun appTypography(): Typography {
    val display = InstrumentSansFamily
    val body = SpaceGroteskFamily

    return Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp
        ),
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 30.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )
}
