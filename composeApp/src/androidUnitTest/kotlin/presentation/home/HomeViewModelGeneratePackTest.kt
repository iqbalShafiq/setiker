package presentation.home

import data.aijob.AiJobManager
import data.auth.AuthManager
import data.remote.StickerApiRepository
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
import domain.model.aijob.AiJob
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftStatus
import domain.model.AiQuotaOperation
import domain.model.AiQuotaReservation
import domain.model.AiUsage
import domain.repository.AiQuotaRepository
import domain.repository.StickerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import presentation.aijob.AiJobEnqueueHelper
import presentation.aijob.DraftResultApplier
import presentation.aijob.ViewModelAiJobTestSupport
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.info_ai_job_started_background
import setiker.composeapp.generated.resources.success_generate_pack_background
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        val harness = homeHarness(packId = "unused")
        val viewModel = homeViewModel(mockk(relaxed = true), mockk(relaxed = true), harness)

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("My Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Me"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)

        val effect = viewModel.effect.first()
        assertTrue(effect is HomeEffect.ShowError)
        assertFalse(viewModel.state.value.isGeneratePackLoading)
        coVerify(exactly = 0) { harness.manager.enqueue(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun generatePackSuccessRefreshesListWithoutNavigation() = runTest {
        val packId = "my_pack_1234"
        val harness = homeHarness(packId = packId)
        val repository = mockk<StickerRepository>(relaxed = true)
        coEvery { repository.getAllPacks() } returns listOf(fakePack(id = packId))
        val viewModel = homeViewModel(repository, mockk(), harness)

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("My Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Me"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPrompt("Cute cat stickers"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isGeneratePackSheetOpen)

        harness.completeJob()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isGeneratePackSheetOpen)
        assertFalse(viewModel.state.value.isGeneratePackLoading)
        coVerify(atLeast = 1) { repository.getAllPacks() }
    }

    @Test
    fun closeWhileGeneratingDoesNotAllowDuplicateRequest() = runTest {
        val harness = homeHarness(packId = "generated", completeImmediately = false)
        val viewModel = homeViewModel(mockk(relaxed = true), mockk(), harness)

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Pub"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPrompt("Prompt"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isGeneratePackLoading)
        assertTrue(viewModel.state.value.homeWorkspaceDraftId != null)

        viewModel.onIntent(HomeIntent.CloseGeneratePackSheet)
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        coVerify(exactly = 1) { harness.manager.enqueue(any(), any(), any(), any(), any(), any(), any()) }
        assertTrue(viewModel.state.value.homeWorkspaceDraftId != null)

        harness.completeJob()
        advanceUntilIdle()
        assertNull(viewModel.state.value.homeWorkspaceDraftId)
    }

    @Test
    fun duplicateSubmitWhileProcessingIsIgnored() = runTest {
        val harness = homeHarness(packId = "generated", completeImmediately = false)
        val viewModel = homeViewModel(mockk(relaxed = true), mockk(), harness)

        viewModel.onIntent(HomeIntent.OpenGeneratePackSheet)
        viewModel.onIntent(HomeIntent.UpdateGeneratePackName("Pack"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPublisher("Pub"))
        viewModel.onIntent(HomeIntent.UpdateGeneratePackPrompt("Prompt"))
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        viewModel.onIntent(HomeIntent.GenerateStickerPack)
        advanceUntilIdle()

        coVerify(exactly = 1) { harness.manager.enqueue(any(), any(), any(), any(), any(), any(), any()) }

        harness.completeJob()
        advanceUntilIdle()
    }

    private fun homeHarness(
        packId: String,
        completeImmediately: Boolean = true
    ): HomeTestHarness {
        val draftFlow = MutableStateFlow<WorkspaceDraft?>(null)
        val jobFlow = MutableStateFlow<AiJob?>(null)
        val applier = mockk<DraftResultApplier> {
            every { observeDraft(any()) } returns draftFlow
            every { observeJobCompletion(any()) } returns jobFlow
            every { applyGeneratePackCompletion(any(), any(), any()) } answers {
                val job = thirdArg<AiJob?>()
                if (job?.status == AiJobStatus.COMPLETED && job.type == AiJobType.GENERATE_PACK) packId else null
            }
        }
        val manager = ViewModelAiJobTestSupport.manager { draft ->
            draftFlow.value = draft.copy(status = WorkspaceDraftStatus.PROCESSING)
            if (completeImmediately) {
                jobFlow.value = completedJob(draft.id)
            }
        }
        coEvery { manager.getDraft(any()) } answers { draftFlow.value }
        every { manager.observeDrafts() } returns draftFlow.map { listOfNotNull(it) }
        every { manager.observeJobs() } returns jobFlow.map { listOfNotNull(it) }
        return HomeTestHarness(
            manager = manager,
            enqueueHelper = ViewModelAiJobTestSupport.enqueueHelper(manager),
            draftResultApplier = applier,
            draftFlow = draftFlow,
            jobFlow = jobFlow,
            packId = packId
        ) {
            val draft = draftFlow.value ?: return@HomeTestHarness
            draftFlow.value = draft.copy(status = WorkspaceDraftStatus.APPLIED)
            jobFlow.value = completedJob(draft.id)
        }
    }

    private fun completedJob(draftId: String): AiJob = mockk(relaxed = true) {
        every { status } returns AiJobStatus.COMPLETED
        every { type } returns AiJobType.GENERATE_PACK
        every { workspaceDraftId } returns draftId
    }

    private fun homeViewModel(
        repository: StickerRepository,
        apiRepository: StickerApiRepository,
        harness: HomeTestHarness
    ) = HomeViewModel(
        repository = repository,
        authManager = fakeAuthManager(),
        syncManager = fakeSyncManager(),
        apiRepository = apiRepository,
        draftSaver = CapturingDraftSaver(fakePack()),
        aiJobManager = harness.manager,
        enqueueHelper = harness.enqueueHelper,
        draftResultApplier = harness.draftResultApplier,
        aiQuotaRepository = fakeAiQuotaRepository()
    )

    private fun fakeAiQuotaRepository(): AiQuotaRepository = object : AiQuotaRepository {
        override suspend fun getUsage(forceRefresh: Boolean): AiUsage? = AiUsage(
            pointLimit = 100,
            pointsRemaining = 100
        )
        override suspend fun reserve(operation: AiQuotaOperation): AiQuotaReservation =
            AiQuotaReservation("test-reservation", operation, 1, 99)
        override suspend fun finalizeCommitted(reservationId: String) = Unit
        override suspend fun finalizeReleased(reservationId: String) = Unit
        override fun invalidateCache() = Unit
    }

    private class HomeTestHarness(
        val manager: AiJobManager,
        val enqueueHelper: AiJobEnqueueHelper,
        val draftResultApplier: DraftResultApplier,
        private val draftFlow: MutableStateFlow<WorkspaceDraft?>,
        private val jobFlow: MutableStateFlow<AiJob?>,
        val packId: String,
        private val complete: () -> Unit
    ) {
        fun completeJob() = complete()
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
