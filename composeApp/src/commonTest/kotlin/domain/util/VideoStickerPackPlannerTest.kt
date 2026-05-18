package domain.util

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
}
