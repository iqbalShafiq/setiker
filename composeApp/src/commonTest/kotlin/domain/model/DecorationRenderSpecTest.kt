package domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecorationRenderSpecTest {
    @Test
    fun bottomCaptionLayoutControlsRenderMetricsWithoutIdPrefix() {
        val decoration = TextDecoration(
            id = "any_future_id",
            text = "HELLO",
            font = DecorationFont.Sans,
            layout = TextDecorationLayout.BottomCaption
        )

        assertTrue(decoration.isBottomCaption())
        assertEquals(6, DecorationRenderSpec.textMaxLines(decoration))
        assertEquals(
            512f * (1f - 2f * DecorationRenderSpec.API_CAPTION_HORIZONTAL_INSET_RATIO),
            DecorationRenderSpec.textBoxWidthPx(
                decoration = decoration,
                canvasWidthPx = 512f,
                minDimPx = 512f,
                scale = 0.58f
            )
        )
        assertEquals(
            512f * 0.46f * 0.58f,
            DecorationRenderSpec.textBoxHeightPx(
                decoration = decoration,
                minDimPx = 512f,
                scale = 0.58f
            )
        )
        assertEquals(
            512f * DecorationRenderSpec.API_CAPTION_TEXT_SIZE_RATIO * 0.58f,
            DecorationRenderSpec.textSizePx(
                decoration = decoration,
                minDimPx = 512f,
                scale = 0.58f
            )
        )
    }

    @Test
    fun apiTxtIdDoesNotControlBottomCaptionMetrics() {
        val decoration = TextDecoration(
            id = "api_txt_legacy",
            text = "HELLO",
            font = DecorationFont.Sans
        )

        assertFalse(decoration.isBottomCaption())
        assertEquals(3, DecorationRenderSpec.textMaxLines(decoration))
        assertEquals(
            512f * DecorationRenderSpec.TEXT_BOX_RATIO,
            DecorationRenderSpec.textBoxWidthPx(
                decoration = decoration,
                canvasWidthPx = 512f,
                minDimPx = 512f,
                scale = 1f
            )
        )
    }

    @Test
    fun layerStrokeWidthUsesWidthRatio() {
        assertEquals(
            48f * 0.08f,
            DecorationRenderSpec.layerStrokeWidthPx(textSizePx = 48f, strokeWidthRatio = 0.08f)
        )
        assertEquals(0f, DecorationRenderSpec.layerStrokeWidthPx(textSizePx = 48f, strokeWidthRatio = 0f))
    }
}
