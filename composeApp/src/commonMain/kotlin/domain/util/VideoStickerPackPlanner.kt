package domain.util

import domain.model.CandidateGridImage
import domain.model.VideoFrameCandidate
import domain.model.VideoStickerPackGridSpec
import domain.model.VideoStickerCandidateManifestItem

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

    fun buildCandidateManifest(
        candidates: List<VideoFrameCandidate>,
        grids: List<CandidateGridImage>
    ): List<VideoStickerCandidateManifestItem> {
        val cappedCandidates = candidates.take(MAX_CANDIDATES)
        require(grids.size <= 2) { "At most 2 candidate grids are supported" }
        require(grids.all { it.layout == GRID_LAYOUT }) { "Candidate grids must use $GRID_LAYOUT layout" }
        require(grids.sumOf { it.frameCount } == cappedCandidates.size) {
            "Candidate manifest size must match candidate grid frame count"
        }

        return cappedCandidates.mapIndexed { index, candidate ->
            val gridIndex = index / GRID_CELL_COUNT
            val cellIndex = index % GRID_CELL_COUNT
            VideoStickerCandidateManifestItem(
                candidateId = "frame_${index.toString().padStart(4, '0')}",
                frameIndex = index,
                gridIndex = gridIndex,
                cellId = cellIdFor(cellIndex),
                timestampMs = candidate.timestampMs,
                sharpnessScore = candidate.sharpnessScore,
                brightnessScore = candidate.brightnessScore,
                differenceScore = candidate.differenceScore
            )
        }
    }

    private fun cellIdFor(cellIndex: Int): String {
        val row = cellIndex / 4
        val col = cellIndex % 4
        return "${'A' + row}${col + 1}"
    }
}
