package presentation.editor

import data.remote.ExploreApiRepository
import data.remote.StickerApiRepository
import data.storage.StickerFileStorage
import data.util.EmojiPreferences
import data.util.OnDeviceImageProcessor
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
import presentation.aijob.EditorAiDeps
import presentation.aijob.ViewModelAiJobTestSupport
import presentation.common.UiText
import presentation.createpack.DraftSticker
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_select_image
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelImproveTest {
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
    fun improveStickerBlankImageEmitsSelectImageErrorAndDoesNotSetLoading() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(EditorIntent.RequestImproveSticker)
        advanceUntilIdle()

        val effect = withTimeout(1_000) { viewModel.effect.first() }
        val error = assertIs<EditorEffect.ShowError>(effect)
        val message = assertIs<UiText.StringRes>(error.message)
        assertEquals(Res.string.error_select_image, message.resource)
        assertFalse(viewModel.state.value.isApiLoading)
    }

    @Test
    fun improveStickerSuccessAppliesGeneratedPreviewWithDecorations() = runTest {
        val decoration = TextDecoration(
            id = "api-text-1",
            text = "HELLO",
            font = DecorationFont.Sans
        )
        val deps = ViewModelAiJobTestSupport.editorDependencies()
        val viewModel = createViewModel(deps)

        viewModel.onIntent(EditorIntent.UpdateImagePath("/tmp/source.png"))
        viewModel.onIntent(EditorIntent.RequestImproveSticker)
        viewModel.onIntent(EditorIntent.ConfirmImproveSticker)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isApiLoading)
        assertEquals("Processing...", viewModel.state.value.backgroundJobMessage)

        val draftId = viewModel.state.value.workspaceDraftId!!
        deps.jobsFlow.value = listOf(
            ViewModelAiJobTestSupport.completeImproveJob(
                draftId = draftId,
                previews = listOf(
                    DraftStickerSnapshot(
                        imagePath = "/tmp/improved.png",
                        decorations = listOf<StickerDecoration>(decoration)
                    )
                ),
                replaceMode = false
            )
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isApiLoading)
        assertTrue(viewModel.state.value.generatedResultsSheetVisible)
        assertEquals("/tmp/improved.png", viewModel.state.value.generatedPreview.single().imagePath)
        assertEquals(listOf(decoration), viewModel.state.value.generatedPreview.single().decorations)
    }

    @Test
    fun improveStickerEnqueuesBackgroundJob() = runTest {
        val deps = ViewModelAiJobTestSupport.editorDependencies()
        val viewModel = createViewModel(deps)

        viewModel.onIntent(EditorIntent.UpdateImagePath("/tmp/source.png"))
        viewModel.onIntent(EditorIntent.RequestImproveSticker)
        viewModel.onIntent(EditorIntent.ConfirmImproveSticker)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isApiLoading)
        assertEquals("Processing...", viewModel.state.value.backgroundJobMessage)
        coVerify(exactly = 1) { deps.manager.enqueue(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun applyGeneratedStickerAppliesDraftAndClearsSelectionAndPreview() = runTest {
        val viewModel = createViewModel()
        val generatedDecoration = TextDecoration(
            id = "api-text-2",
            text = "CAPTION",
            font = DecorationFont.Serif
        )
        val draft = DraftSticker(
            imagePath = "/tmp/improved.png",
            decorations = listOf(generatedDecoration)
        )

        viewModel.onIntent(EditorIntent.UpdateImagePath("/tmp/source.png"))
        viewModel.onIntent(EditorIntent.AddTextDecoration("Local", domain.model.TextDecorationStyle.ClassicOutline))
        assertTrue(viewModel.state.value.selectedDecorationId != null)
        viewModel.onIntent(EditorIntent.ApplyGeneratedSticker(draft))

        assertEquals("/tmp/improved.png", viewModel.state.value.imagePath)
        assertEquals(listOf(generatedDecoration), viewModel.state.value.decorations)
        assertNull(viewModel.state.value.selectedDecorationId)
        assertTrue(viewModel.state.value.generatedPreview.isEmpty())
    }

    private fun createViewModel(
        deps: EditorAiDeps = ViewModelAiJobTestSupport.editorDependencies(),
        apiRepository: StickerApiRepository = mockk(relaxed = true)
    ): EditorViewModel {
        val repository = mockk<StickerRepository>(relaxed = true)
        val emojiPreferences = mockk<EmojiPreferences>(relaxed = true)
        val fileStorage = mockk<StickerFileStorage>(relaxed = true)
        val onDeviceImageProcessor = mockk<OnDeviceImageProcessor>(relaxed = true)
        return EditorViewModel(
            repository = repository,
            emojiPreferences = emojiPreferences,
            fileStorage = fileStorage,
            apiRepository = apiRepository,
            onDeviceImageProcessor = onDeviceImageProcessor,
            aiJobManager = deps.manager,
            enqueueHelper = deps.enqueueHelper,
            draftResultApplier = deps.draftResultApplier,
            aiQuotaRepository = mockk<AiQuotaRepository>(relaxed = true),
            exploreApiRepository = mockk<ExploreApiRepository>(relaxed = true)
        )
    }
}
