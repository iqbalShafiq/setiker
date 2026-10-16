package data.remote

import data.storage.StickerFileStorage
import data.util.OnDeviceImageProcessor
import domain.model.CandidateGridImage
import domain.model.VideoFrameCandidate
import domain.model.VideoStickerCandidateManifestItem
import domain.util.VideoStickerPackPlanner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StickerApiRepositoryVideoStickerPackTest {
    @Test
    fun mapsStaticPlanCandidatesToLocalFramePaths() = runTest {
        val api = mockk<SetikerApiService>()
        val repository = StickerApiRepository(
            api = api,
            fileStorage = mockk<StickerFileStorage>(relaxed = true),
            onDeviceImageProcessor = mockk<OnDeviceImageProcessor>(relaxed = true)
        )
        val candidates = listOf(
            VideoFrameCandidate("/tmp/frame_0.png", 1_000L, 0.9, 0.5, 0.2),
            VideoFrameCandidate("/tmp/frame_1.png", 2_000L, 0.8, 0.6, 0.3)
        )
        val grids = listOf(CandidateGridImage("/tmp/grid.png", frameCount = 2))
        val manifest = VideoStickerPackPlanner.buildCandidateManifest(candidates, grids)
        coEvery {
            api.generateVideoStickerPack(any(), any(), any(), any(), any(), any())
        } returns data.remote.model.ApiVideoStickerPackPlan(
            packTitle = "Plan",
            staticStickers = listOf(
                data.remote.model.ApiVideoStaticStickerPlan(
                    candidateId = "frame_0001",
                    frameIndex = 1,
                    timestampMs = 2_000L,
                    cellId = "A2",
                    emojis = listOf("😀")
                )
            )
        )

        val result = repository.generateVideoStickerPack(
            candidateGridPaths = grids.map { it.filePath },
            candidateManifest = manifest,
            candidates = candidates,
            selectedStartMs = 0L,
            selectedEndMs = 10_000L,
            sourceDurationMs = 10_000L,
            prompt = "funny"
        )

        assertEquals("Plan", result.plan.packTitle)
        assertEquals("/tmp/frame_1.png", result.staticStickers.single().localPath)
        coVerify(exactly = 0) { api.downloadImageBytes(any()) }
    }

    @Test
    fun resolvesCandidatePathsByManifestFrameIndexWhenManifestOrderDiffers() = runTest {
        val api = mockk<SetikerApiService>()
        val repository = StickerApiRepository(
            api = api,
            fileStorage = mockk<StickerFileStorage>(relaxed = true),
            onDeviceImageProcessor = mockk<OnDeviceImageProcessor>(relaxed = true)
        )
        val candidates = listOf(
            VideoFrameCandidate("/tmp/frame_0.png", 1_000L, 0.9, 0.5, 0.2),
            VideoFrameCandidate("/tmp/frame_1.png", 2_000L, 0.8, 0.6, 0.3)
        )
        val manifest = listOf(
            VideoStickerCandidateManifestItem("frame_0001", 1, 0, "A2", 2_000L, 0.8, 0.6, 0.3),
            VideoStickerCandidateManifestItem("frame_0000", 0, 0, "A1", 1_000L, 0.9, 0.5, 0.2)
        )
        coEvery {
            api.generateVideoStickerPack(any(), any(), any(), any(), any(), any())
        } returns data.remote.model.ApiVideoStickerPackPlan(
            packTitle = "Plan",
            staticStickers = listOf(
                data.remote.model.ApiVideoStaticStickerPlan(
                    candidateId = "frame_0001",
                    frameIndex = 1,
                    timestampMs = 2_000L,
                    cellId = "A2"
                )
            )
        )

        val result = repository.generateVideoStickerPack(
            candidateGridPaths = listOf("/tmp/grid.png"),
            candidateManifest = manifest,
            candidates = candidates,
            selectedStartMs = 0L,
            selectedEndMs = 10_000L,
            sourceDurationMs = 10_000L
        )

        assertEquals("/tmp/frame_1.png", result.staticStickers.single().localPath)
    }

    @Test
    fun rejectsPlanWithUnknownCandidateId() = runTest {
        val api = mockk<SetikerApiService>()
        val repository = StickerApiRepository(
            api = api,
            fileStorage = mockk<StickerFileStorage>(relaxed = true),
            onDeviceImageProcessor = mockk<OnDeviceImageProcessor>(relaxed = true)
        )
        val candidates = listOf(VideoFrameCandidate("/tmp/frame_0.png", 1_000L, 0.9, 0.5, 0.2))
        val grids = listOf(CandidateGridImage("/tmp/grid.png", frameCount = 1))
        val manifest = listOf(
            VideoStickerCandidateManifestItem(
                candidateId = "frame_0000",
                frameIndex = 0,
                gridIndex = 0,
                cellId = "A1",
                timestampMs = 1_000L,
                sharpnessScore = 0.9,
                brightnessScore = 0.5,
                differenceScore = 0.2
            )
        )
        coEvery {
            api.generateVideoStickerPack(any(), any(), any(), any(), any(), any())
        } returns data.remote.model.ApiVideoStickerPackPlan(
            packTitle = "Plan",
            staticStickers = listOf(
                data.remote.model.ApiVideoStaticStickerPlan(
                    candidateId = "missing",
                    frameIndex = 99,
                    timestampMs = 9_999L,
                    cellId = "D4",
                    emojis = emptyList()
                )
            )
        )

        assertFailsWith<ApiException> {
            repository.generateVideoStickerPack(
                candidateGridPaths = grids.map { it.filePath },
                candidateManifest = manifest,
                candidates = candidates,
                selectedStartMs = 0L,
                selectedEndMs = 10_000L,
                sourceDurationMs = 10_000L
            )
        }
    }
}
