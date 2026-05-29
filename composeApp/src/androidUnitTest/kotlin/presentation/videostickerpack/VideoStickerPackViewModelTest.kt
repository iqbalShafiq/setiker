package presentation.videostickerpack

import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
import domain.model.CandidateGridImage
import domain.model.DecodedFrame
import domain.model.ResolvedVideoAnimatedSticker
import domain.model.ResolvedVideoAnimatedTimelineFrame
import domain.model.ResolvedVideoStaticSticker
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.Sticker
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.model.VideoFrameCandidate
import domain.model.VideoAnimatedStickerPlan
import domain.model.VideoAnimatedTimelineFrame
import domain.model.VideoStaticStickerPlan
import domain.model.VideoStickerPackPlan
import domain.repository.StickerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class VideoStickerPackViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun regenerateReusesExistingCandidateGrids() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.getVideoDurationMs(any()) } returns 120_000L
        coEvery { fileStorage.extractVideoFrameToFile(any(), any(), any()) } returns "/tmp/video_preview.png"
        val extractor = mockk<VideoFrameCandidateExtractor>()
        coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
            VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3)
        )
        val gridComposer = mockk<CandidateGridComposer>()
        coEvery { gridComposer.composeGrids(any()) } returns listOf(
            CandidateGridImage("/tmp/grid1.png", frameCount = 1)
        )
        val apiRepository = mockk<StickerApiRepository>()
        coEvery {
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any())
        } returns resolvedPlan("/tmp/out.png")

        val viewModel = VideoStickerPackViewModel(
            fileStorage = fileStorage,
            extractor = extractor,
            gridComposer = gridComposer,
            apiRepository = apiRepository,
            stickerRepository = mockk(relaxed = true),
            draftSaver = CapturingDraftSaver(fakePack())
        )

        viewModel.onIntent(VideoStickerPackIntent.LoadVideo("/tmp/video.mp4"))
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.Generate)
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.Regenerate)
        advanceUntilIdle()

        coVerify(exactly = 1) { extractor.extractCandidates(any(), any(), any(), any()) }
        coVerify(exactly = 1) { gridComposer.composeGrids(any()) }
        coVerify(exactly = 2) {
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun savePackDoesNotRunBeforeGeneratedPreviewExists() = runTest {
        val repository = mockk<StickerRepository>(relaxed = true)
        val viewModel = VideoStickerPackViewModel(
            fileStorage = mockk<StickerFileStorage>(relaxed = true),
            extractor = mockk<VideoFrameCandidateExtractor>(relaxed = true),
            gridComposer = mockk<CandidateGridComposer>(relaxed = true),
            apiRepository = mockk<StickerApiRepository>(relaxed = true),
            stickerRepository = repository,
            draftSaver = CapturingDraftSaver(fakePack())
        )

        viewModel.onIntent(VideoStickerPackIntent.UpdatePackName("My Pack"))
        viewModel.onIntent(VideoStickerPackIntent.UpdatePublisher("Me"))
        viewModel.onIntent(VideoStickerPackIntent.SavePack)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.savePack(any()) }
    }

    @Test
    fun savePackPersistsGeneratedStickersAfterPreview() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.getVideoDurationMs(any()) } returns 60_000L
        coEvery { fileStorage.extractVideoFrameToFile(any(), any(), any()) } returns "/tmp/video_preview.png"
        coEvery { fileStorage.loadImage(any()) } returns null
        val extractor = mockk<VideoFrameCandidateExtractor>()
        coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
            VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3)
        )
        val gridComposer = mockk<CandidateGridComposer>()
        coEvery { gridComposer.composeGrids(any()) } returns listOf(CandidateGridImage("/tmp/grid.png", 1))
        val apiRepository = mockk<StickerApiRepository>()
        coEvery {
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any())
        } returns resolvedPlan("/tmp/first.png", "/tmp/second.png")
        val repository = mockk<StickerRepository>(relaxed = true)
        val saver = CapturingDraftSaver(fakePack(id = "video_pack_1"))
        val viewModel = VideoStickerPackViewModel(
            fileStorage = fileStorage,
            extractor = extractor,
            gridComposer = gridComposer,
            apiRepository = apiRepository,
            stickerRepository = repository,
            draftSaver = saver
        )

        viewModel.onIntent(VideoStickerPackIntent.LoadVideo("/tmp/video.mp4"))
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.Generate)
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.UpdatePackName("Video Pack"))
        viewModel.onIntent(VideoStickerPackIntent.UpdatePublisher("Setiker"))
        viewModel.onIntent(VideoStickerPackIntent.SavePack)
        advanceUntilIdle()

        val captured = assertNotNull(saver.lastInput)
        assertEquals("Video Pack", captured.name)
        assertEquals("Setiker", captured.publisher)
        assertEquals("/tmp/first.png", captured.trayImagePath)
        assertEquals(2, captured.stickers.size)
        assertEquals("/tmp/first.png", captured.stickers[0].imagePath)
        assertEquals("/tmp/second.png", captured.stickers[1].imagePath)
        coVerify(exactly = 1) { repository.savePack(any()) }

        val effect = viewModel.effect.first()
        assertEquals(VideoStickerPackEffect.NavigateToPackDetail("video_pack_1"), effect)
    }

    @Test
    fun saveAnimatedPlanFallsBackToVideoDecodeWhenAnyTimelineFrameFileIsMissing() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.getVideoDurationMs(any()) } returns 60_000L
        coEvery { fileStorage.extractVideoFrameToFile(any(), any(), any()) } returns "/tmp/video_preview.png"
        val extractor = mockk<VideoFrameCandidateExtractor>()
        coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
            VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3),
            VideoFrameCandidate("/tmp/c2.png", 2_000L, 0.4, 0.5, 0.3),
            VideoFrameCandidate("/tmp/c3.png", 3_000L, 0.4, 0.5, 0.3)
        )
        val gridComposer = mockk<CandidateGridComposer>()
        coEvery { gridComposer.composeGrids(any()) } returns listOf(CandidateGridImage("/tmp/grid.png", 3))
        coEvery { fileStorage.loadImage("/tmp/c1.png") } returns byteArrayOf(1)
        coEvery { fileStorage.loadImage("/tmp/c2.png") } returns null
        coEvery { fileStorage.loadImage("/tmp/c3.png") } returns byteArrayOf(3)
        coEvery { fileStorage.decodeVideoFrames(any(), any(), any()) } returns listOf(
            DecodedFrame(byteArrayOf(10), 83L),
            DecodedFrame(byteArrayOf(11), 83L)
        )
        coEvery { fileStorage.saveAnimatedStickerImage(any(), any(), any(), any(), any()) } returns "/tmp/anim.webp"
        val apiRepository = mockk<StickerApiRepository>()
        coEvery {
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any())
        } returns animatedResolvedPlan()
        val repository = mockk<StickerRepository>(relaxed = true)
        val saver = CapturingDraftSaver(fakePack(id = "video_pack_anim"))
        val viewModel = VideoStickerPackViewModel(
            fileStorage = fileStorage,
            extractor = extractor,
            gridComposer = gridComposer,
            apiRepository = apiRepository,
            stickerRepository = repository,
            draftSaver = saver
        )

        viewModel.onIntent(VideoStickerPackIntent.LoadVideo("/tmp/video.mp4"))
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.Generate)
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.UpdatePackName("Video Pack"))
        viewModel.onIntent(VideoStickerPackIntent.UpdatePublisher("Setiker"))
        viewModel.onIntent(VideoStickerPackIntent.SavePack)
        advanceUntilIdle()

        coVerify(exactly = 1) { fileStorage.decodeVideoFrames(any(), any(), any()) }
        coVerify(exactly = 1) {
            fileStorage.saveAnimatedStickerImage(
                frames = listOf(DecodedFrame(byteArrayOf(10), 83L), DecodedFrame(byteArrayOf(11), 83L)),
                fileName = any(),
                baseDecorations = emptyList(),
                frameDecorations = emptyMap(),
                onProgress = any()
            )
        }
        assertEquals(true, assertNotNull(saver.lastInput).stickers.single().isAnimated)
    }

    @Test
    fun generateSelectsAllGeneratedVideoStickersByDefault() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.getVideoDurationMs(any()) } returns 60_000L
        val extractor = mockk<VideoFrameCandidateExtractor>()
        coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
            VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3),
            VideoFrameCandidate("/tmp/c2.png", 2_000L, 0.4, 0.5, 0.3)
        )
        val gridComposer = mockk<CandidateGridComposer>()
        coEvery { gridComposer.composeGrids(any()) } returns listOf(CandidateGridImage("/tmp/grid.png", 2))
        val apiRepository = mockk<StickerApiRepository>()
        coEvery {
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any())
        } returns mixedResolvedPlan()
        val viewModel = VideoStickerPackViewModel(
            fileStorage = fileStorage,
            extractor = extractor,
            gridComposer = gridComposer,
            apiRepository = apiRepository,
            stickerRepository = mockk(relaxed = true),
            draftSaver = CapturingDraftSaver(fakePack())
        )

        viewModel.onIntent(VideoStickerPackIntent.LoadVideo("/tmp/video.mp4"))
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.Generate)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.selectedStaticStickerKeys.size)
        assertEquals(1, state.selectedAnimatedStickerKeys.size)
        assertEquals(2, state.selectedStickerCount)
    }

    @Test
    fun saveMixedVideoSelectionPersistsStaticAndAnimatedPacksSeparately() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.getVideoDurationMs(any()) } returns 60_000L
        coEvery { fileStorage.loadImage("/tmp/a1.png") } returns byteArrayOf(1)
        coEvery { fileStorage.loadImage("/tmp/a2.png") } returns byteArrayOf(2)
        coEvery { fileStorage.saveAnimatedStickerImage(any(), any(), any(), any(), any()) } returns "/tmp/animated.webp"
        val extractor = mockk<VideoFrameCandidateExtractor>()
        coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
            VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3),
            VideoFrameCandidate("/tmp/c2.png", 2_000L, 0.4, 0.5, 0.3)
        )
        val gridComposer = mockk<CandidateGridComposer>()
        coEvery { gridComposer.composeGrids(any()) } returns listOf(CandidateGridImage("/tmp/grid.png", 2))
        val apiRepository = mockk<StickerApiRepository>()
        coEvery {
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any())
        } returns mixedResolvedPlan()
        val repository = mockk<StickerRepository>(relaxed = true)
        val saver = MultiCapturingDraftSaver()
        val viewModel = VideoStickerPackViewModel(
            fileStorage = fileStorage,
            extractor = extractor,
            gridComposer = gridComposer,
            apiRepository = apiRepository,
            stickerRepository = repository,
            draftSaver = saver
        )

        viewModel.onIntent(VideoStickerPackIntent.LoadVideo("/tmp/video.mp4"))
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.Generate)
        advanceUntilIdle()
        viewModel.onIntent(VideoStickerPackIntent.UpdatePackName("Video Pack"))
        viewModel.onIntent(VideoStickerPackIntent.UpdatePublisher("Setiker"))
        viewModel.onIntent(VideoStickerPackIntent.SavePack)
        advanceUntilIdle()

        assertEquals(2, saver.inputs.size)
        assertEquals("Video Pack Static", saver.inputs[0].name)
        assertEquals("Video Pack Animated", saver.inputs[1].name)
        assertEquals(false, saver.inputs[0].stickers.single().isAnimated)
        assertEquals(true, saver.inputs[1].stickers.single().isAnimated)
        assertEquals(true, saver.inputs[0].identifier.endsWith("_static"))
        assertEquals(true, saver.inputs[1].identifier.endsWith("_animated"))
        coVerify(exactly = 2) { repository.savePack(any()) }
    }

    private fun fakePack(id: String = "id"): StickerPack = StickerPack(
        identifier = id,
        name = "Name",
        publisher = "Publisher",
        trayImageFile = "/tmp/tray.png",
        stickers = listOf(Sticker(imageFile = "/tmp/a.png"))
    )

    private fun resolvedPlan(vararg paths: String): ResolvedVideoStickerPackPlan {
        val staticStickers = paths.mapIndexed { index, path ->
            ResolvedVideoStaticSticker(
                plan = VideoStaticStickerPlan(
                    candidateId = "frame_${index.toString().padStart(4, '0')}",
                    frameIndex = index,
                    timestampMs = index * 1000L,
                    cellId = "A${index + 1}",
                    emojis = emptyList()
                ),
                localPath = path
            )
        }
        return ResolvedVideoStickerPackPlan(
            plan = VideoStickerPackPlan(
                packTitle = "Plan",
                staticStickers = staticStickers.map { it.plan }
            ),
            staticStickers = staticStickers,
            animatedStickers = emptyList()
        )
    }

    private fun animatedResolvedPlan(): ResolvedVideoStickerPackPlan {
        val frames = listOf(
            VideoAnimatedTimelineFrame("frame_0000", 0, 1_000L, 83L),
            VideoAnimatedTimelineFrame("frame_0001", 1, 2_000L, 83L),
            VideoAnimatedTimelineFrame("frame_0002", 2, 3_000L, 83L)
        )
        val animatedPlan = VideoAnimatedStickerPlan(
            timeline = frames,
            fps = 12,
            loopCount = 0,
            emojis = emptyList()
        )
        return ResolvedVideoStickerPackPlan(
            plan = VideoStickerPackPlan(packTitle = "Plan", animatedStickers = listOf(animatedPlan)),
            staticStickers = emptyList(),
            animatedStickers = listOf(
                ResolvedVideoAnimatedSticker(
                    plan = animatedPlan,
                    timeline = listOf(
                        ResolvedVideoAnimatedTimelineFrame(frames[0], "/tmp/c1.png"),
                        ResolvedVideoAnimatedTimelineFrame(frames[1], "/tmp/c2.png"),
                        ResolvedVideoAnimatedTimelineFrame(frames[2], "/tmp/c3.png")
                    )
                )
            )
        )
    }

    private fun mixedResolvedPlan(): ResolvedVideoStickerPackPlan {
        val static = ResolvedVideoStaticSticker(
            plan = VideoStaticStickerPlan(
                candidateId = "frame_0000",
                frameIndex = 0,
                timestampMs = 1_000L,
                cellId = "A1",
                emojis = emptyList()
            ),
            localPath = "/tmp/static.png"
        )
        val frames = listOf(
            VideoAnimatedTimelineFrame("frame_0001", 1, 2_000L, 83L),
            VideoAnimatedTimelineFrame("frame_0002", 2, 3_000L, 83L)
        )
        val animatedPlan = VideoAnimatedStickerPlan(
            timeline = frames,
            fps = 12,
            loopCount = 0,
            emojis = emptyList()
        )
        return ResolvedVideoStickerPackPlan(
            plan = VideoStickerPackPlan(
                packTitle = "Plan",
                staticStickers = listOf(static.plan),
                animatedStickers = listOf(animatedPlan)
            ),
            staticStickers = listOf(static),
            animatedStickers = listOf(
                ResolvedVideoAnimatedSticker(
                    plan = animatedPlan,
                    timeline = listOf(
                        ResolvedVideoAnimatedTimelineFrame(frames[0], "/tmp/a1.png"),
                        ResolvedVideoAnimatedTimelineFrame(frames[1], "/tmp/a2.png")
                    )
                )
            )
        )
    }

    private class CapturingDraftSaver(
        private val resultPack: StickerPack
    ) : StickerPackDraftSaver(fileStorage = mockk<StickerFileStorage>(relaxed = true)) {
        var lastInput: StickerDraftInput? = null

        override suspend fun buildDraftPack(input: StickerDraftInput): StickerPack {
            lastInput = input
            return resultPack
        }
    }

    private class MultiCapturingDraftSaver : StickerPackDraftSaver(fileStorage = mockk<StickerFileStorage>(relaxed = true)) {
        val inputs = mutableListOf<StickerDraftInput>()

        override suspend fun buildDraftPack(input: StickerDraftInput): StickerPack {
            inputs += input
            return StickerPack(
                identifier = input.identifier,
                name = input.name,
                publisher = input.publisher,
                trayImageFile = input.trayImagePath,
                stickers = input.stickers.map { Sticker(imageFile = it.imagePath, isAnimated = it.isAnimated) },
                isAnimated = input.stickers.any { it.isAnimated },
                visibility = input.visibility
            )
        }
    }
}
