package presentation.createpack

import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import domain.model.DecorationFont
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.aijob.DraftStickerSnapshot
import domain.repository.StickerRepository
import domain.repository.AiQuotaRepository
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
import kotlinx.coroutines.withTimeout
import presentation.aijob.CreatePackAiDeps
import presentation.aijob.ViewModelAiJobTestSupport
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_no_static_stickers_to_improve
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePackViewModelImproveTest {
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
    fun improvePackWithoutStaticStickersEmitsError() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(
            CreatePackIntent.AddAnimatedDraft(
                DraftSticker(imagePath = "/tmp/a.webp", isAnimated = true)
            )
        )

        viewModel.onIntent(CreatePackIntent.RequestImprovePackStickers)
        advanceUntilIdle()

        val effect = withTimeout(1_000) { viewModel.effect.first() }
        val error = assertIs<CreatePackEffect.ShowError>(effect)
        val message = assertIs<UiText.StringRes>(error.message)
        assertEquals(Res.string.error_no_static_stickers_to_improve, message.resource)
        assertFalse(viewModel.state.value.isApiLoading)
    }

    @Test
    fun improvePackUsesOnlyStaticSourcesAndOpensReplacePreview() = runTest {
        val decoration = TextDecoration(id = "api-text-1", text = "HELLO", font = DecorationFont.Sans)
        val deps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = createViewModel(deps)
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static-1.png"))
        viewModel.onIntent(CreatePackIntent.AddSticker(""))
        viewModel.onIntent(CreatePackIntent.AddAnimatedDraft(DraftSticker(imagePath = "/tmp/a.webp", isAnimated = true)))
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static-2.png"))

        viewModel.onIntent(CreatePackIntent.RequestImprovePackStickers)
        viewModel.onIntent(CreatePackIntent.ConfirmImprovePackStickers)
        advanceUntilIdle()
        val draftId = viewModel.state.value.workspaceDraftId!!
        deps.jobsFlow.value = listOf(
            ViewModelAiJobTestSupport.completeImproveJob(
                draftId = draftId,
                previews = listOf(
                    DraftStickerSnapshot(
                        imagePath = "/tmp/improved-1.png",
                        decorations = listOf<StickerDecoration>(decoration)
                    ),
                    DraftStickerSnapshot(imagePath = "/tmp/improved-2.png")
                ),
                replaceMode = true
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { deps.manager.enqueue(any(), any(), any(), any(), any(), any(), any()) }
        assertFalse(viewModel.state.value.isApiLoading)
        assertTrue(viewModel.state.value.generatedResultsSheetVisible)
        assertEquals(GeneratedPreviewMode.ReplacePack, viewModel.state.value.generatedPreviewMode)
        assertEquals(2, viewModel.state.value.generatedPreview.size)
        assertEquals(setOf(0, 1), viewModel.state.value.selectedGeneratedPreview)
        assertEquals(listOf(decoration), viewModel.state.value.generatedPreview.first().decorations)
    }

    @Test
    fun replacePackWithGeneratedKeepsAnimatedAndClearsPreviewState() = runTest {
        val deps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = createViewModel(deps)
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static-1.png"))
        viewModel.onIntent(CreatePackIntent.AddAnimatedDraft(DraftSticker(imagePath = "/tmp/a.webp", isAnimated = true)))
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static-2.png"))

        viewModel.onIntent(CreatePackIntent.RequestImprovePackStickers)
        viewModel.onIntent(CreatePackIntent.ConfirmImprovePackStickers)
        advanceUntilIdle()
        val draftId = viewModel.state.value.workspaceDraftId!!
        deps.jobsFlow.value = listOf(
            ViewModelAiJobTestSupport.completeImproveJob(
                draftId = draftId,
                previews = listOf(
                    DraftStickerSnapshot(imagePath = "/tmp/improved-1.png"),
                    DraftStickerSnapshot(imagePath = "/tmp/improved-2.png")
                ),
                replaceMode = true
            )
        )
        advanceUntilIdle()
        viewModel.onIntent(CreatePackIntent.ToggleGeneratedSelection(1))

        viewModel.onIntent(CreatePackIntent.ReplacePackWithGenerated)

        val stickers = viewModel.state.value.stickers
        assertEquals(listOf("/tmp/improved-1.png", "/tmp/a.webp"), stickers.map { it.imagePath })
        assertTrue(stickers.last().isAnimated)
        assertTrue(viewModel.state.value.generatedPreview.isEmpty())
        assertTrue(viewModel.state.value.selectedGeneratedPreview.isEmpty())
        assertEquals(GeneratedPreviewMode.AddToPack, viewModel.state.value.generatedPreviewMode)
    }

    @Test
    fun generateEnqueuesBackgroundJob() = runTest {
        val deps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = createViewModel(deps)
        viewModel.onIntent(CreatePackIntent.UpdateGeneratePrompt("cats"))

        viewModel.onIntent(CreatePackIntent.GenerateStickers)
        advanceUntilIdle()

        coVerify(exactly = 1) { deps.manager.enqueue(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun improveEnqueuesBackgroundJob() = runTest {
        val deps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = createViewModel(deps)
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static.png"))

        viewModel.onIntent(CreatePackIntent.RequestImprovePackStickers)
        viewModel.onIntent(CreatePackIntent.ConfirmImprovePackStickers)
        advanceUntilIdle()

        coVerify(exactly = 1) { deps.manager.enqueue(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun toggleGeneratedSelectionOutOfRangeIsIgnored() = runTest {
        val deps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = createViewModel(deps)
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static.png"))
        viewModel.onIntent(CreatePackIntent.RequestImprovePackStickers)
        viewModel.onIntent(CreatePackIntent.ConfirmImprovePackStickers)
        advanceUntilIdle()
        val draftId = viewModel.state.value.workspaceDraftId!!
        deps.jobsFlow.value = listOf(
            ViewModelAiJobTestSupport.completeImproveJob(
                draftId = draftId,
                previews = listOf(DraftStickerSnapshot(imagePath = "/tmp/improved-1.png")),
                replaceMode = true
            )
        )
        advanceUntilIdle()

        val before = viewModel.state.value.selectedGeneratedPreview
        viewModel.onIntent(CreatePackIntent.ToggleGeneratedSelection(9))

        assertEquals(before, viewModel.state.value.selectedGeneratedPreview)
    }

    @Test
    fun dismissGeneratedResultsSheetKeepsPreviewForReopen() = runTest {
        val deps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = createViewModel(deps)
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static.png"))
        viewModel.onIntent(CreatePackIntent.RequestImprovePackStickers)
        viewModel.onIntent(CreatePackIntent.ConfirmImprovePackStickers)
        advanceUntilIdle()
        val draftId = viewModel.state.value.workspaceDraftId!!
        deps.jobsFlow.value = listOf(
            ViewModelAiJobTestSupport.completeImproveJob(
                draftId = draftId,
                previews = listOf(DraftStickerSnapshot(imagePath = "/tmp/improved-1.png")),
                replaceMode = true
            )
        )
        advanceUntilIdle()

        viewModel.onIntent(CreatePackIntent.DismissGeneratedResultsSheet)

        assertFalse(viewModel.state.value.generatedResultsSheetVisible)
        assertEquals(1, viewModel.state.value.generatedPreview.size)
    }

    @Test
    fun replacePackWithGeneratedNoOpWhenEffectiveSelectionEmpty() = runTest {
        val deps = ViewModelAiJobTestSupport.createPackDependencies()
        val viewModel = createViewModel(deps)
        viewModel.onIntent(CreatePackIntent.AddSticker("/tmp/static-1.png"))
        viewModel.onIntent(CreatePackIntent.AddAnimatedDraft(DraftSticker(imagePath = "/tmp/a.webp", isAnimated = true)))
        viewModel.onIntent(CreatePackIntent.RequestImprovePackStickers)
        viewModel.onIntent(CreatePackIntent.ConfirmImprovePackStickers)
        advanceUntilIdle()
        val draftId = viewModel.state.value.workspaceDraftId!!
        deps.jobsFlow.value = listOf(
            ViewModelAiJobTestSupport.completeImproveJob(
                draftId = draftId,
                previews = listOf(DraftStickerSnapshot(imagePath = "/tmp/improved-1.png")),
                replaceMode = true
            )
        )
        advanceUntilIdle()

        viewModel.onIntent(CreatePackIntent.ToggleGeneratedSelection(0))
        val before = viewModel.state.value
        viewModel.onIntent(CreatePackIntent.ReplacePackWithGenerated)

        assertEquals(before.stickers, viewModel.state.value.stickers)
        assertEquals(before.generatedPreview, viewModel.state.value.generatedPreview)
        assertEquals(before.selectedGeneratedPreview, viewModel.state.value.selectedGeneratedPreview)
        assertEquals(before.generatedPreviewMode, viewModel.state.value.generatedPreviewMode)
    }

    private fun createViewModel(
        deps: CreatePackAiDeps = ViewModelAiJobTestSupport.createPackDependencies(),
        apiRepository: StickerApiRepository = mockk(relaxed = true)
    ): CreatePackViewModel {
        val repository = mockk<StickerRepository>(relaxed = true)
        val fileStorage = mockk<StickerFileStorage>(relaxed = true)
        val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })
        return CreatePackViewModel(
            repository = repository,
            apiRepository = apiRepository,
            draftSaver = saver,
            aiJobManager = deps.manager,
            enqueueHelper = deps.enqueueHelper,
            draftResultApplier = deps.draftResultApplier,
            jobRepository = deps.jobRepository,
            aiQuotaRepository = mockk<AiQuotaRepository>(relaxed = true),
            authManager = mockk(relaxed = true)
        )
    }
}
