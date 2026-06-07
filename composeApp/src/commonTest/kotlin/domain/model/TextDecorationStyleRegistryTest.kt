package domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextDecorationStyleRegistryTest {
    @Test
    fun selectableStylesExcludeCustomAndApiCaption() {
        val styles = TextDecorationStyleRegistry.selectableStyles
        assertTrue(TextDecorationStyle.ClassicOutline in styles)
        assertTrue(TextDecorationStyle.StickerPop in styles)
        assertTrue(TextDecorationStyle.Custom !in styles)
        assertTrue(TextDecorationStyle.ApiCaption !in styles)
    }

    @Test
    fun presetStrokeRatiosAreWithinBounds() {
        TextDecorationStyleRegistry.selectableStyles.forEach { style ->
            val preset = TextDecorationStyleRegistry.preset(style)
            preset.layers.forEach { layer ->
                layer.strokeWidthRatio?.let { ratio ->
                    assertTrue(ratio in 0f..0.2f, "Invalid stroke ratio for $style")
                }
            }
        }
    }

    @Test
    fun stickerPopHasFillLayer() {
        val preset = TextDecorationStyleRegistry.preset(TextDecorationStyle.StickerPop)
        assertEquals(DecorationFont.Bungee, preset.font)
        assertTrue(preset.layers.any { it.useFillColor })
    }

    @Test
    fun sunsetGradientHasGradientFillLayer() {
        val preset = TextDecorationStyleRegistry.preset(TextDecorationStyle.SunsetGradient)
        assertTrue(
            preset.layers.any {
                it.useFillColor && it.gradientStartArgb != null && it.gradientEndArgb != null
            }
        )
    }
}
