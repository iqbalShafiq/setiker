package presentation.videostickerpack

import data.remote.StickerApiRepository
import data.remote.model.GridSplitStickerFile
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
import domain.model.CandidateGridImage
import domain.model.Sticker
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.model.VideoFrameCandidate
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
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any())
        } returns listOf(GridSplitStickerFile(localPath = "/tmp/out.png"))

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
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any())
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
        val extractor = mockk<VideoFrameCandidateExtractor>()
        coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
            VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3)
        )
        val gridComposer = mockk<CandidateGridComposer>()
        coEvery { gridComposer.composeGrids(any()) } returns listOf(CandidateGridImage("/tmp/grid.png", 1))
        val apiRepository = mockk<StickerApiRepository>()
        coEvery {
            apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any())
        } returns listOf(
            GridSplitStickerFile(localPath = "/tmp/first.png"),
            GridSplitStickerFile(localPath = "/tmp/second.png")
        )
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

    private fun fakePack(id: String = "id"): StickerPack = StickerPack(
        identifier = id,
        name = "Name",
        publisher = "Publisher",
        trayImageFile = "/tmp/tray.png",
        stickers = listOf(Sticker(imageFile = "/tmp/a.png"))
    )

    private class CapturingDraftSaver(
        private val resultPack: StickerPack
    ) : StickerPackDraftSaver(fileStorage = mockk<StickerFileStorage>(relaxed = true)) {
        var lastInput: StickerDraftInput? = null

        override suspend fun buildDraftPack(input: StickerDraftInput): StickerPack {
            lastInput = input
            return resultPack
        }
    }
}
