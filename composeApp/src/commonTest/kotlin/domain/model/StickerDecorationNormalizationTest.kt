package domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class StickerDecorationNormalizationTest {
    @Test
    fun decodeStickerDecorationsForCurrentSchemaNormalizesLegacyApiCaptionIds() {
        val decorations = decodeStickerDecorationsForCurrentSchema(
            """
            [
              {
                "type": "text",
                "id": "api_txt_legacy",
                "text": "OLD",
                "font": "Sans",
                "centerX": 0.5,
                "centerY": 0.88,
                "scale": 0.58
              }
            ]
            """.trimIndent()
        )

        val decoration = decorations.single() as TextDecoration
        assertEquals(TextDecorationSource.ApiOutsideForeground, decoration.source)
        assertEquals(TextDecorationLayout.BottomCaption, decoration.layout)
        assertEquals(TextDecorationStyle.ApiCaption, decoration.style)
    }

    @Test
    fun decodePreservesExplicitStyleField() {
        val decorations = decodeStickerDecorationsForCurrentSchema(
            """
            [
              {
                "type": "text",
                "id": "styled",
                "text": "POP",
                "font": "Bungee",
                "style": "StickerPop",
                "centerX": 0.5,
                "centerY": 0.5,
                "scale": 1.0
              }
            ]
            """.trimIndent()
        )

        val decoration = decorations.single() as TextDecoration
        assertEquals(TextDecorationStyle.StickerPop, decoration.style)
        assertEquals(DecorationFont.Bungee, decoration.font)
    }

    @Test
    fun decodeInfersCustomStyleFromLegacyBorderOverrides() {
        val decorations = decodeStickerDecorationsForCurrentSchema(
            """
            [
              {
                "type": "text",
                "id": "custom_border",
                "text": "Hi",
                "font": "Sans",
                "borderWidthRatio": 0.14,
                "centerX": 0.5,
                "centerY": 0.5,
                "scale": 1.0
              }
            ]
            """.trimIndent()
        )

        val decoration = decorations.single() as TextDecoration
        assertEquals(TextDecorationStyle.Custom, decoration.style)
    }

    @Test
    fun decodeInfersClassicOutlineWhenLegacyFieldsMatchCurrentPreset() {
        val decorations = decodeStickerDecorationsForCurrentSchema(
            """
            [
              {
                "type": "text",
                "id": "plain",
                "text": "OK",
                "font": "Fredoka",
                "fontWeight": "Bold",
                "centerX": 0.5,
                "centerY": 0.5,
                "scale": 1.0
              }
            ]
            """.trimIndent()
        )

        val decoration = decorations.single() as TextDecoration
        assertEquals(TextDecorationStyle.ClassicOutline, decoration.style)
    }

    @Test
    fun decodeInfersCustomForLegacySansFont() {
        val decorations = decodeStickerDecorationsForCurrentSchema(
            """
            [
              {
                "type": "text",
                "id": "legacy_sans",
                "text": "OK",
                "font": "Sans",
                "centerX": 0.5,
                "centerY": 0.5,
                "scale": 1.0
              }
            ]
            """.trimIndent()
        )

        val decoration = decorations.single() as TextDecoration
        assertEquals(TextDecorationStyle.Custom, decoration.style)
    }
}
