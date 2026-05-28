package domain.util

import domain.model.CandidateGridImage
import domain.model.VideoFrameCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VideoStickerPackPlannerTest {
    @Test
    fun validateRangeAcceptsSixtySeconds() {
        VideoStickerPackPlanner.validateSelectedRange(10_000L, 70_000L)
    }

    @Test
    fun validateRangeRejectsLongerThanSixtySeconds() {
        assertFailsWith<IllegalArgumentException> {
            VideoStickerPackPlanner.validateSelectedRange(0L, 60_001L)
        }
    }

    @Test
    fun sampleTimestampsAreUniformAndInsideRange() {
        val timestamps = VideoStickerPackPlanner.buildSampleTimestamps(
            startMs = 1_000L,
            endMs = 61_000L,
            sampleCount = 5
        )

        assertEquals(listOf(1_000L, 16_000L, 31_000L, 46_000L, 61_000L), timestamps)
    }

    @Test
    fun sampleTimestampsRejectNonPositiveSampleCount() {
        assertFailsWith<IllegalArgumentException> {
            VideoStickerPackPlanner.buildSampleTimestamps(
                startMs = 1_000L,
                endMs = 61_000L,
                sampleCount = 0
            )
        }
    }

    @Test
    fun sampleTimestampsWithSingleSampleReturnsStart() {
        val timestamps = VideoStickerPackPlanner.buildSampleTimestamps(
            startMs = 1_000L,
            endMs = 61_000L,
            sampleCount = 1
        )

        assertEquals(listOf(1_000L), timestamps)
    }

    @Test
    fun candidatePathsAreCappedAtThirtyTwo() {
        val paths = (1..40).map { "/tmp/$it.png" }
        assertEquals(32, VideoStickerPackPlanner.capCandidatePaths(paths).size)
    }

    @Test
    fun candidatesBatchIntoTwoFourByFourGrids() {
        val paths = (1..32).map { "/tmp/$it.png" }
        val batches = VideoStickerPackPlanner.batchCandidatePathsForGrids(paths)

        assertEquals(2, batches.size)
        assertTrue(batches.all { it.size == 16 })
    }

    @Test
    fun candidatesBatchKeepsRemainderWhenNotMultipleOfGridSize() {
        val paths = (1..18).map { "/tmp/$it.png" }
        val batches = VideoStickerPackPlanner.batchCandidatePathsForGrids(paths)

        assertEquals(2, batches.size)
        assertEquals(16, batches[0].size)
        assertEquals(2, batches[1].size)
    }

    @Test
    fun buildCandidateManifestAssignsStableIdsGridIndexesAndCellIds() {
        val candidates = (0 until 18).map { index ->
            VideoFrameCandidate(
                filePath = "/tmp/frame_$index.png",
                timestampMs = index * 1000L,
                sharpnessScore = index + 0.1,
                brightnessScore = index + 0.2,
                differenceScore = index + 0.3
            )
        }
        val grids = listOf(
            CandidateGridImage(filePath = "/tmp/grid_0.png", frameCount = 16),
            CandidateGridImage(filePath = "/tmp/grid_1.png", frameCount = 2)
        )

        val manifest = VideoStickerPackPlanner.buildCandidateManifest(candidates, grids)

        assertEquals(18, manifest.size)
        assertEquals("frame_0000", manifest[0].candidateId)
        assertEquals(0, manifest[0].frameIndex)
        assertEquals(0, manifest[0].gridIndex)
        assertEquals("A1", manifest[0].cellId)
        assertEquals("frame_0015", manifest[15].candidateId)
        assertEquals(0, manifest[15].gridIndex)
        assertEquals("D4", manifest[15].cellId)
        assertEquals("frame_0016", manifest[16].candidateId)
        assertEquals(1, manifest[16].gridIndex)
        assertEquals("A1", manifest[16].cellId)
        assertEquals(17_000L, manifest[17].timestampMs)
        assertEquals(17.1, manifest[17].sharpnessScore)
    }

    @Test
    fun buildCandidateManifestRejectsGridAndCandidateCountMismatch() {
        val candidates = listOf(
            VideoFrameCandidate("/tmp/frame_0.png", 0L, 1.0, 1.0, 1.0)
        )
        val grids = listOf(CandidateGridImage(filePath = "/tmp/grid_0.png", frameCount = 2))

        assertFailsWith<IllegalArgumentException> {
            VideoStickerPackPlanner.buildCandidateManifest(candidates, grids)
        }
    }
}
