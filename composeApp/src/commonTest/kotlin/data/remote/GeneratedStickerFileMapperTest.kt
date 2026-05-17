package data.remote

import data.remote.model.ApiImage
import data.remote.model.ApiTextAssetDecoration
import domain.error.AppErrorCode
import domain.model.TextDecoration
import domain.model.TextDecorationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeneratedStickerFileMapperTest {
    @Test
    fun apiImageMapsDownloadedPathAndDecorationsToGeneratedStickerFile() {
        val file = ApiImage(
            id = "image-1",
            url = "/uploads/image-1.png",
            textAssetDecoration = ApiTextAssetDecoration(text = "HELLO")
        ).toGeneratedStickerFile(localPath = "/local/image-1.png")

        assertEquals("/local/image-1.png", file.localPath)
        val decoration = file.decorations.single() as TextDecoration
        assertEquals("HELLO", decoration.text)
        assertEquals(TextDecorationSource.ApiTextAsset, decoration.source)
    }

    @Test
    fun improveGridResponseCountMismatchThrowsInvalidGenerateResponse() {
        val error = assertFailsWith<ApiException> {
            validateImproveGridResponseCount(
                images = listOf(ApiImage(id = "grid-1", url = "/uploads/grid-1.png")),
                chunkSizes = listOf(16, 2)
            )
        }

        assertEquals(AppErrorCode.InvalidGenerateResponse, error.code)
    }
}
