package presentation.home

import data.auth.AuthManager
import data.remote.StickerApiRepository
import data.remote.model.GridSplitStickerFile
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import data.sync.SyncManager
import domain.model.AuthState
import domain.model.Sticker
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.model.SyncOperation
import domain.model.SyncReport
import domain.model.SyncStage
import domain.repository.StickerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelGeneratePackTest {
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
    fun generatePackFailsWhenPromptBlank() = runTest {
        val repository = mockk<StickerRepository>(relaxed = true)
        val apiRepository = mockk<StickerApiRepository>(relaxed = true)
        val saver = CapturingDraftSaver(savedPack = fakePack())
        val viewModel = HomeViewModel(
            repository = repository,
            authManager = fakeAuthManager(),
            syncManager = fakeSyncManager(),
            apiRepository = apiRepository,
            draftSaver = saver
        )

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("My Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Me"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)

        val effect = viewModel.effect.first()
        assertTrue(effect is HomeEffect.ShowError)
        assertFalse(viewModel.state.value.isGeneratePackLoading)
        coVerify(exactly = 0) { apiRepository.generateStickerPack(any(), any(), any()) }
    }

    @Test
    fun generatePackSuccessSavesAndNavigates() = runTest {
        val repository = mockk<StickerRepository>(relaxed = true)
        coEvery { repository.getAllPacks() } returns listOf(fakePack())
        val apiRepository = mockk<StickerApiRepository>()
        coEvery {
            apiRepository.generateStickerPack(
                prompt = any(),
                layout = any(),
                inputImagePath = any()
            )
        } returns listOf(
            GridSplitStickerFile(localPath = "/tmp/first.png"),
            GridSplitStickerFile(localPath = "/tmp/second.png")
        )

        val builtPack = fakePack(id = "my_pack_1234")
        val saver = CapturingDraftSaver(savedPack = builtPack)
        val viewModel = HomeViewModel(
            repository = repository,
            authManager = fakeAuthManager(),
            syncManager = fakeSyncManager(),
            apiRepository = apiRepository,
            draftSaver = saver
        )

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("My Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Me"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPrompt("Cute cat stickers"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        val capturedInput = assertNotNull(saver.lastInput)
        assertEquals("My Pack", capturedInput.name)
        assertEquals("Me", capturedInput.publisher)
        assertEquals("PRIVATE", capturedInput.visibility)
        assertEquals("/tmp/first.png", capturedInput.trayImagePath)
        assertEquals(2, capturedInput.stickers.size)
        assertEquals("/tmp/first.png", capturedInput.stickers[0].imagePath)
        assertEquals("/tmp/second.png", capturedInput.stickers[1].imagePath)

        assertFalse(viewModel.state.value.isGeneratePackSheetOpen)
        assertFalse(viewModel.state.value.isGeneratePackLoading)
        assertEquals("", viewModel.state.value.generatePackPrompt)
        assertNull(viewModel.state.value.generatePackInputImagePath)

        coVerify(exactly = 1) { repository.savePack(builtPack) }
        coVerify(atLeast = 1) { repository.getAllPacks() }

        val effect = viewModel.effect.first()
        assertEquals(HomeEffect.NavigateToPackDetail(packId = builtPack.identifier), effect)
    }

    @Test
    fun closeWhileGeneratingDoesNotAllowDuplicateRequest() = runTest {
        val repository = mockk<StickerRepository>(relaxed = true)
        coEvery { repository.getAllPacks() } returns emptyList()
        val gate = CompletableDeferred<Unit>()
        val apiRepository = mockk<StickerApiRepository>()
        coEvery { apiRepository.generateStickerPack(any(), any(), any()) } coAnswers {
            gate.await()
            listOf(GridSplitStickerFile(localPath = "/tmp/only.png"))
        }
        val viewModel = HomeViewModel(
            repository = repository,
            authManager = fakeAuthManager(),
            syncManager = fakeSyncManager(),
            apiRepository = apiRepository,
            draftSaver = CapturingDraftSaver(fakePack("generated"))
        )

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Pub"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPrompt("Prompt"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isGeneratePackLoading)

        viewModel.onIntent(HomeIntent.CloseGeneratePackSheet)
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        coVerify(exactly = 1) { apiRepository.generateStickerPack(any(), any(), any()) }
        assertTrue(viewModel.state.value.isGeneratePackLoading)
        assertFalse(viewModel.state.value.isGeneratePackSheetOpen)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isGeneratePackLoading)
    }

    @Test
    fun duplicateSubmitWhileLoadingIsIgnored() = runTest {
        val repository = mockk<StickerRepository>(relaxed = true)
        val gate = CompletableDeferred<Unit>()
        val apiRepository = mockk<StickerApiRepository>()
        coEvery { apiRepository.generateStickerPack(any(), any(), any()) } coAnswers {
            gate.await()
            listOf(GridSplitStickerFile(localPath = "/tmp/only.png"))
        }
        val viewModel = HomeViewModel(
            repository = repository,
            authManager = fakeAuthManager(),
            syncManager = fakeSyncManager(),
            apiRepository = apiRepository,
            draftSaver = CapturingDraftSaver(fakePack("generated"))
        )

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Pub"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPrompt("Prompt"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        coVerify(exactly = 1) { apiRepository.generateStickerPack(any(), any(), any()) }

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun generatePackFailureResetsLoadingAndKeepsSheetOpen() = runTest {
        val repository = mockk<StickerRepository>(relaxed = true)
        val apiRepository = mockk<StickerApiRepository>()
        coEvery { apiRepository.generateStickerPack(any(), any(), any()) } throws IllegalStateException("boom")
        val viewModel = HomeViewModel(
            repository = repository,
            authManager = fakeAuthManager(),
            syncManager = fakeSyncManager(),
            apiRepository = apiRepository,
            draftSaver = CapturingDraftSaver(fakePack())
        )

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Pub"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPrompt("Prompt"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is HomeEffect.ShowError)
        assertFalse(viewModel.state.value.isGeneratePackLoading)
        assertTrue(viewModel.state.value.isGeneratePackSheetOpen)
    }

    private fun fakeAuthManager(): AuthManager {
        val authState = MutableStateFlow<AuthState>(AuthState.UNAUTHENTICATED)
        return mockk {
            every { this@mockk.authState } returns authState
            every { this@mockk.currentUser } returns emptyFlow()
        }
    }

    private fun fakeSyncManager(): SyncManager {
        return mockk {
            every { operationsFlow } returns emptyFlow<List<SyncOperation>>()
            every { activeOperationFlow } returns MutableStateFlow(null)
            every { isSyncing } returns MutableStateFlow(false)
            every { lastSyncReport } returns MutableStateFlow<SyncReport?>(null)
            every { syncStage } returns MutableStateFlow(SyncStage.IDLE)
            coEvery { sync() } returns SyncReport()
        }
    }

    private fun fakePack(id: String = "id"): StickerPack = StickerPack(
        identifier = id,
        name = "Name",
        publisher = "Publisher",
        trayImageFile = "/tmp/tray.png",
        stickers = listOf(Sticker(imageFile = "/tmp/a.png"))
    )

    private class CapturingDraftSaver(
        private val savedPack: StickerPack
    ) : StickerPackDraftSaver(fileStorage = mockk<StickerFileStorage>(relaxed = true)) {
        var lastInput: StickerDraftInput? = null

        override suspend fun buildDraftPack(input: StickerDraftInput): StickerPack {
            lastInput = input
            return savedPack
        }
    }
}
