package presentation.createpack

import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import domain.model.DecorationFont
import domain.model.Sticker
import domain.model.StickerDecoration
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.model.TextDecoration
import domain.repository.StickerRepository
import domain.repository.AiQuotaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import presentation.aijob.ViewModelAiJobTestSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePackViewModelSavePackTest {
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
    fun savePackDelegatesMappedDraftInputToSaver() = runTest {
        val repository = mockk<StickerRepository>(relaxed = true)
        val apiRepository = mockk<StickerApiRepository>()
        coEvery { apiRepository.improve(any()) } returns emptyList()
        coEvery { apiRepository.generateStickers(any(), any()) } returns emptyList()

        val decoration = TextDecoration(id = "t1", text = "hello", font = DecorationFont.Sans)
        val knownPack = StickerPack(
            identifier = "known-pack-id",
            name = "Known",
            publisher = "Known Publisher",
            trayImageFile = "/saved/tray.webp",
            stickers = listOf(Sticker(imageFile = "/saved/sticker.webp"))
        )
        val saver = CapturingDraftSaver(knownPack)

        val aiDeps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = CreatePackViewModel(
            repository = repository,
            apiRepository = apiRepository,
            draftSaver = saver,
            aiJobManager = aiDeps.manager,
            enqueueHelper = aiDeps.enqueueHelper,
            draftResultApplier = aiDeps.draftResultApplier,
            jobRepository = aiDeps.jobRepository,
            aiQuotaRepository = mockk<AiQuotaRepository>(relaxed = true)
        )

        viewModel.onIntent(CreatePackIntent.UpdateName("My Pack"))
        viewModel.onIntent(CreatePackIntent.UpdatePublisher("My Publisher"))
        viewModel.onIntent(CreatePackIntent.UpdateVisibility("PUBLIC"))
        viewModel.onIntent(CreatePackIntent.UpdateTrayImage("/tmp/tray.png"))
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static.png"))
        viewModel.onIntent(
            CreatePackIntent.AddAnimatedDraft(
                DraftSticker(
                    imagePath = "/tmp/anim.webp",
                    decorations = listOf(decoration),
                    isAnimated = true,
                    sourceVideoFile = "/tmp/source.mp4",
                    frameDecorations = mapOf(0 to listOf(decoration))
                )
            )
        )

        viewModel.onIntent(CreatePackIntent.SavePack)
        advanceUntilIdle()

        val captured = assertNotNull(saver.lastInput)
        assertTrue(captured.identifier.isNotBlank())
        assertEquals("PUBLIC", captured.visibility)
        assertEquals("/tmp/tray.png", captured.trayImagePath)
        assertEquals(2, captured.stickers.size)
        assertEquals("/tmp/static.png", captured.stickers[0].imagePath)
        assertTrue(captured.stickers[0].decorations.isEmpty())
        assertTrue(captured.stickers[1].isAnimated)
        assertEquals(listOf(decoration), captured.stickers[1].decorations)
        assertEquals("/tmp/source.mp4", captured.stickers[1].sourceVideoFile)
        assertEquals(mapOf(0 to listOf(decoration)), captured.stickers[1].frameDecorations)

        coVerify(exactly = 1) { repository.savePack(knownPack) }
        assertFalse(viewModel.state.value.isSaving)
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
}
