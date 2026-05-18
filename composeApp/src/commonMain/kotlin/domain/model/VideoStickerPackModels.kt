package domain.model

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
