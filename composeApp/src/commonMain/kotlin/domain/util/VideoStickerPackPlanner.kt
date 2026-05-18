package domain.util

import domain.model.VideoStickerPackGridSpec

object VideoStickerPackPlanner {
    const val MAX_SEGMENT_MS: Long = 60_000L
    const val MAX_CANDIDATES: Int = 32
    const val GRID_CELL_COUNT: Int = VideoStickerPackGridSpec.CELL_COUNT
    const val GRID_LAYOUT: String = VideoStickerPackGridSpec.LAYOUT
    const val DEFAULT_RAW_SAMPLE_COUNT: Int = 64

    fun validateSelectedRange(startMs: Long, endMs: Long) {
        require(startMs >= 0L) { "Selected start must be non-negative" }
        require(endMs > startMs) { "Selected end must be greater than start" }
        require(endMs - startMs <= MAX_SEGMENT_MS) { "Selected video segment must be at most 60000 ms" }
    }

    fun buildSampleTimestamps(
        startMs: Long,
        endMs: Long,
        sampleCount: Int = DEFAULT_RAW_SAMPLE_COUNT
    ): List<Long> {
        validateSelectedRange(startMs, endMs)
        require(sampleCount > 0) { "Sample count must be positive" }
        val count = sampleCount
        if (count == 1) return listOf(startMs)
        val span = endMs - startMs
        return (0 until count).map { index ->
            startMs + index.toLong() * span / (count - 1)
        }
    }

    fun capCandidatePaths(paths: List<String>): List<String> = paths.take(MAX_CANDIDATES)

    fun batchCandidatePathsForGrids(paths: List<String>): List<List<String>> =
        capCandidatePaths(paths)
            .chunked(GRID_CELL_COUNT)
            .take(2)
}
