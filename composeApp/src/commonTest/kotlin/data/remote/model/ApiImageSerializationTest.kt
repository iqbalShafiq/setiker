package data.remote.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiImageSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesTextAssetDecorationAlongsideTextOutsideForeground() {
        val image = json.decodeFromString<ApiImage>(
            """
            {
              "id": "image-1",
              "url": "/images/1.png",
              "textOutsideForeground": {
                "text": "OLD",
                "style": { "fontFamily": "Inter", "color": "#111111", "weight": "700" }
              },
              "textAssetDecoration": {
                "text": "NEW",
                "style": { "fontFamily": "Inter", "color": "#222222", "weight": "600" },
                "source": "shared"
              }
            }
            """.trimIndent()
        )

        assertEquals("OLD", image.textOutsideForeground?.text)
        assertEquals("NEW", image.textAssetDecoration?.text)
        assertEquals("#222222", image.textAssetDecoration?.style?.color)
        assertEquals("shared", image.textAssetDecoration?.source)
    }
}
