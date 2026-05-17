package data.remote.mapper

import data.remote.model.ApiImage
import data.remote.model.ApiTextAssetDecoration
import data.remote.model.ApiTextOutsideForeground
import data.remote.model.ApiTextOutsideForegroundStyle
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.TextDecoration
import domain.model.TextDecorationLayout
import domain.model.TextDecorationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiDecorationMapperTest {
    @Test
    fun textAssetDecorationMapsToBottomCaptionDecoration() {
        val decorations = ApiImage(
            id = "image-1",
            url = "/uploads/a.png",
            textAssetDecoration = ApiTextAssetDecoration(
                text = "HELLO",
                style = ApiTextOutsideForegroundStyle(
                    fontFamily = "serif",
                    color = "#FF0000",
                    weight = "bold"
                ),
                source = "detected"
            )
        ).toStickerDecorations()

        val decoration = decorations.single() as TextDecoration
        assertEquals("HELLO", decoration.text)
        assertEquals(DecorationFont.Serif, decoration.font)
        assertEquals(DecorationFontWeight.Bold, decoration.fontWeight)
        assertEquals(0xFFFF0000L, decoration.textColorArgb)
        assertEquals(TextDecorationSource.ApiTextAsset, decoration.source)
        assertEquals(TextDecorationLayout.BottomCaption, decoration.layout)
        assertEquals(0.5f, decoration.centerX)
        assertEquals(0.88f, decoration.centerY)
        assertEquals(0.58f, decoration.scale)
    }

    @Test
    fun outsideForegroundDecorationMapsToSameBottomCaptionGeometry() {
        val decorations = ApiImage(
            id = "image-1",
            url = "/uploads/a.png",
            textOutsideForeground = ApiTextOutsideForeground(
                text = "CAPTION",
                style = ApiTextOutsideForegroundStyle(color = "white")
            )
        ).toStickerDecorations()

        val decoration = decorations.single() as TextDecoration
        assertEquals("CAPTION", decoration.text)
        assertEquals(0xFFFFFFFFL, decoration.textColorArgb)
        assertEquals(TextDecorationSource.ApiOutsideForeground, decoration.source)
        assertEquals(TextDecorationLayout.BottomCaption, decoration.layout)
        assertEquals(0.5f, decoration.centerX)
        assertEquals(0.88f, decoration.centerY)
        assertEquals(0.58f, decoration.scale)
    }

    @Test
    fun blankTextAssetDecorationReturnsNoDecorations() {
        val decorations = ApiImage(
            id = "image-1",
            url = "/uploads/a.png",
            textAssetDecoration = ApiTextAssetDecoration(text = " ")
        ).toStickerDecorations()

        assertTrue(decorations.isEmpty())
    }
}
