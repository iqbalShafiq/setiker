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
    }
}
