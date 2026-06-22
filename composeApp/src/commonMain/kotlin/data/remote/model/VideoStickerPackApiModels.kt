package data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class VideoStickerPackPlanData(
    val plan: ApiVideoStickerPackPlan,
    val metadata: JsonObject? = null
)

@Serializable
data class ApiVideoStickerPackPlan(
    val packTitle: String = "Video Sticker Pack",
    val summary: String? = null,
    val staticStickers: List<ApiVideoStaticStickerPlan> = emptyList(),
    val animatedStickers: List<ApiVideoAnimatedStickerPlan> = emptyList(),
    val rejectedCandidates: List<ApiVideoRejectedCandidatePlan> = emptyList()
)

@Serializable
data class ApiVideoStaticStickerPlan(
    val candidateId: String,
    val frameIndex: Int,
    val timestampMs: Long,
    val cellId: String,
    val emojis: List<String> = emptyList(),
    val accessibilityText: String? = null,
    val decorations: List<ApiVideoStickerDecoration> = emptyList(),
    val rationale: String? = null
)

@Serializable
data class ApiVideoAnimatedStickerPlan(
    val timeline: List<ApiVideoAnimatedTimelineFrame> = emptyList(),
    val fps: Int = 12,
    val loopCount: Int = 0,
    val emojis: List<String> = emptyList(),
    val accessibilityText: String? = null,
    val baseDecorations: List<ApiVideoStickerDecoration> = emptyList(),
    val frameDecorations: List<ApiVideoAnimatedFrameDecorations> = emptyList(),
    val rationale: String? = null
)

@Serializable
data class ApiVideoAnimatedTimelineFrame(
    val candidateId: String? = null,
    val frameIndex: Int,
    val timestampMs: Long,
    val durationMs: Long
)

@Serializable
data class ApiVideoAnimatedFrameDecorations(
    val frameIndex: Int,
    val decorations: List<ApiVideoStickerDecoration> = emptyList()
)

@Serializable
data class ApiVideoRejectedCandidatePlan(
    val candidateId: String,
    val reason: String
)

@Serializable
sealed interface ApiVideoStickerDecoration

@Serializable
@SerialName("text")
data class ApiVideoTextDecoration(
    val text: String,
    val style: ApiTextOutsideForegroundStyle? = null,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.88f,
    val scale: Float = 0.58f
) : ApiVideoStickerDecoration

@Serializable
@SerialName("emoji")
data class ApiVideoEmojiDecoration(
    val emoji: String,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val scale: Float = 1f
) : ApiVideoStickerDecoration
