package data.remote.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoStickerPackPlanSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesVideoStickerPackPlanResponse() {
        val envelope = json.decodeFromString<ApiSuccessEnvelope<VideoStickerPackPlanData>>(
            """
            {
              "success": true,
              "data": {
                "plan": {
                  "packTitle": "Best moments",
                  "summary": "A compact plan",
                  "staticStickers": [
                    {
                      "candidateId": "frame_0001",
                      "frameIndex": 1,
                      "timestampMs": 1200,
                      "cellId": "A2",
                      "emojis": ["😀"],
                      "accessibilityText": "Smiling reaction",
                      "decorations": [
                        { "type": "text", "text": "WOW", "style": { "fontFamily": "Inter", "color": "#fff", "weight": "700" }, "centerX": 0.5, "centerY": 0.88, "scale": 0.6 }
                      ],
                      "rationale": "Clear expression"
                    }
                  ],
                  "animatedStickers": [
                    {
                      "timeline": [
                        { "candidateId": "frame_0001", "frameIndex": 1, "timestampMs": 1200, "durationMs": 83 },
                        { "candidateId": "frame_0002", "frameIndex": 2, "timestampMs": 1283, "durationMs": 83 }
                      ],
                      "fps": 12,
                      "loopCount": 0,
                      "emojis": ["⭐"],
                      "accessibilityText": "Short loop",
                      "baseDecorations": [
                        { "type": "emoji", "emoji": "🔥", "centerX": 0.8, "centerY": 0.2, "scale": 0.7 }
                      ],
                      "frameDecorations": [
                        { "frameIndex": 0, "decorations": [{ "type": "text", "text": "GO" }] }
                      ],
                      "rationale": "Smooth motion"
                    }
                  ],
                  "rejectedCandidates": [
                    { "candidateId": "frame_0009", "reason": "Too blurry" }
                  ]
                },
                "metadata": { "mode": "video-sticker-pack", "candidateCount": 2, "future": true }
              }
            }
            """.trimIndent()
        )

        val plan = envelope.data?.plan
        assertEquals("Best moments", plan?.packTitle)
        assertEquals("WOW", (plan?.staticStickers?.first()?.decorations?.first() as ApiVideoTextDecoration).text)
        assertEquals("🔥", (plan.animatedStickers.first().baseDecorations.first() as ApiVideoEmojiDecoration).emoji)
        assertEquals("frame_0009", plan.rejectedCandidates.first().candidateId)
    }
}
