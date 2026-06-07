package domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecorationStyleResolverTest {
    @Test
    fun presetResolutionPreservesFillOverride() {
        val decoration = TextDecoration(
            id = "t1",
            text = "Hi",
            font = DecorationFont.Sans,
            textColorArgb = 0xFFFF0000L,
            style = TextDecorationStyle.ClassicOutline
        )
        val resolved = decoration.resolveStyle(textSizePx = 48f)
        assertEquals(0xFFFF0000L, resolved.textColorArgb)
        assertTrue(resolved.layers.any { it.isFill })
    }

    @Test
    fun customStyleUsesLegacyBorderFields() {
        val decoration = TextDecoration(
            id = "t1",
            text = "Hi",
            font = DecorationFont.Sans,
            textColorArgb = 0xFF00FF00L,
            borderColorArgb = 0xFF0000FFL,
            borderWidthRatio = 0.1f,
            style = TextDecorationStyle.Custom
        )
        val resolved = decoration.resolveStyle(textSizePx = 50f)
        assertEquals(1, resolved.layers.count { !it.isFill })
        assertEquals(1, resolved.layers.count { it.isFill })
        assertEquals(5f, resolved.layers.first { !it.isFill }.strokeWidthPx)
    }

    @Test
    fun applyStylePresetKeepsFillColor() {
        val decoration = TextDecoration(
            id = "t1",
            text = "Hi",
            font = DecorationFont.Sans,
            textColorArgb = 0xFFFF00FFL,
            style = TextDecorationStyle.Minimal
        )
        val updated = decoration.applyStylePreset(TextDecorationStyle.StickerPop, preserveTextColor = true)
        assertEquals(TextDecorationStyle.StickerPop, updated.style)
        assertEquals(0xFFFF00FFL, updated.textColorArgb)
        assertEquals(DecorationFont.Bungee, updated.font)
    }

    @Test
    fun apiDecorationInfersApiCaptionStyle() {
        val decoration = TextDecoration(
            id = "api_txt_1",
            text = "CAP",
            font = DecorationFont.Sans,
            source = TextDecorationSource.ApiOutsideForeground,
            layout = TextDecorationLayout.BottomCaption
        )
        assertEquals(TextDecorationStyle.ApiCaption, decoration.inferStyleFromLegacyFields())
    }
}
