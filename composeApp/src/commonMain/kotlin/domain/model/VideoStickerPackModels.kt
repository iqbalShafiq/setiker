package domain.model

import kotlinx.serialization.Serializable

data class VideoStickerPackRange(
    val startMs: Long,
    val endMs: Long,
    val sourceDurationMs: Long
) {
    init {
        require(startMs >= 0L) { "Range start must be non-negative" }
        require(endMs > startMs) { "Range end must be greater than start" }
        require(sourceDurationMs >= endMs) { "Source duration must be at least range end" }
    }

    val durationMs: Long get() = endMs - startMs
}

object VideoStickerPackGridSpec {
    const val CELL_COUNT: Int = 16
    const val LAYOUT: String = "4x4"
}

data class VideoFrameCandidate(
    val filePath: String,
    val timestampMs: Long,
    val sharpnessScore: Double,
    val brightnessScore: Double,
    val differenceScore: Double
)

data class CandidateGridImage(
    val filePath: String,
    val frameCount: Int,
    val layout: String = VideoStickerPackGridSpec.LAYOUT
)

@Serializable
data class VideoStickerCandidateManifestItem(
    val candidateId: String,
    val frameIndex: Int,
    val gridIndex: Int,
    val cellId: String,
    val timestampMs: Long,
    val sharpnessScore: Double,
    val brightnessScore: Double,
    val differenceScore: Double
)

data class VideoStickerPackPlan(
    val packTitle: String,
    val summary: String? = null,
    val staticStickers: List<VideoStaticStickerPlan> = emptyList(),
    val animatedStickers: List<VideoAnimatedStickerPlan> = emptyList(),
    val rejectedCandidates: List<VideoRejectedCandidatePlan> = emptyList()
)

data class VideoStaticStickerPlan(
    val candidateId: String,
    val frameIndex: Int,
    val timestampMs: Long,
    val cellId: String,
    val emojis: List<String>,
    val accessibilityText: String? = null,
    val decorations: List<StickerDecoration> = emptyList(),
    val rationale: String? = null
)

data class VideoAnimatedStickerPlan(
    val timeline: List<VideoAnimatedTimelineFrame>,
    val fps: Int,
    val loopCount: Int,
    val emojis: List<String>,
    val accessibilityText: String? = null,
    val baseDecorations: List<StickerDecoration> = emptyList(),
    val frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap(),
    val rationale: String? = null
)

data class VideoAnimatedTimelineFrame(
    val candidateId: String?,
    val frameIndex: Int,
    val timestampMs: Long,
    val durationMs: Long
)

data class VideoRejectedCandidatePlan(
    val candidateId: String,
    val reason: String
)

data class ResolvedVideoStickerPackPlan(
    val plan: VideoStickerPackPlan,
    val staticStickers: List<ResolvedVideoStaticSticker>,
    val animatedStickers: List<ResolvedVideoAnimatedSticker>
)

data class ResolvedVideoStaticSticker(
    val plan: VideoStaticStickerPlan,
    val localPath: String
)

data class ResolvedVideoAnimatedSticker(
    val plan: VideoAnimatedStickerPlan,
    val timeline: List<ResolvedVideoAnimatedTimelineFrame>
)

data class ResolvedVideoAnimatedTimelineFrame(
    val frame: VideoAnimatedTimelineFrame,
    val localPath: String
)
