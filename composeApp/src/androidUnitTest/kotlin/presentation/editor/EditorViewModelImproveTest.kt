package presentation.editor

import data.remote.StickerApiRepository
import data.remote.model.GeneratedStickerFile
import data.storage.StickerFileStorage
import data.util.EmojiPreferences
import data.util.OnDeviceImageProcessor
import domain.model.DecorationFont
import domain.model.TextDecoration
import domain.repository.StickerRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import presentation.common.UiText
import presentation.createpack.DraftSticker
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_improve_sticker
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

        viewModel.onIntent(EditorIntent.ImproveSticker)
        advanceUntilIdle()

        val effect = withTimeout(1_000) { viewModel.effect.first() }
        val error = assertIs<EditorEffect.ShowError>(effect)
        val message = assertIs<UiText.StringRes>(error.message)
        assertEquals(Res.string.error_select_image, message.resource)
        assertFalse(viewModel.state.value.isApiLoading)
    }

    @Test
    fun improveStickerSuccessTogglesLoadingAndUpdatesGeneratedPreviewWithDecorations() = runTest {
        val apiRepository = mockk<StickerApiRepository>()
        val decoration = TextDecoration(
            id = "api-text-1",
            text = "HELLO",
            font = DecorationFont.Sans
        )
        coEvery { apiRepository.improve(listOf("/tmp/source.png")) } coAnswers {
            kotlinx.coroutines.delay(1_000)
            listOf(
            GeneratedStickerFile(
                localPath = "/tmp/improved.png",
                decorations = listOf(decoration)
            )
            )
        }
        val viewModel = createViewModel(apiRepository = apiRepository)

        viewModel.onIntent(EditorIntent.UpdateImagePath("/tmp/source.png"))
        viewModel.onIntent(EditorIntent.ImproveSticker)
        runCurrent()
        assertTrue(viewModel.state.value.isApiLoading)
        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isApiLoading)
        assertEquals("/tmp/improved.png", viewModel.state.value.generatedPreview.single().imagePath)
        assertEquals(listOf(decoration), viewModel.state.value.generatedPreview.single().decorations)
    }

    @Test
    fun improveStickerFailureTogglesLoadingOffAndEmitsImproveError() = runTest {
        val apiRepository = mockk<StickerApiRepository>()
        coEvery { apiRepository.improve(listOf("/tmp/source.png")) } coAnswers {
            kotlinx.coroutines.delay(1_000)
            throw RuntimeException("boom")
        }
        val viewModel = createViewModel(apiRepository = apiRepository)

        viewModel.onIntent(EditorIntent.UpdateImagePath("/tmp/source.png"))
        viewModel.onIntent(EditorIntent.ImproveSticker)
        runCurrent()
        assertTrue(viewModel.state.value.isApiLoading)
        advanceTimeBy(1_000)
        advanceUntilIdle()

        val effect = withTimeout(1_000) { viewModel.effect.first() }
        val error = assertIs<EditorEffect.ShowError>(effect)
        val message = assertIs<UiText.StringRes>(error.message)
        assertEquals(Res.string.error_failed_improve_sticker, message.resource)
        assertFalse(viewModel.state.value.isApiLoading)
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
        viewModel.onIntent(EditorIntent.AddTextDecoration("Local", DecorationFont.Sans))
        assertTrue(viewModel.state.value.selectedDecorationId != null)
        viewModel.onIntent(EditorIntent.ApplyGeneratedSticker(draft))

        assertEquals("/tmp/improved.png", viewModel.state.value.imagePath)
        assertEquals(listOf(generatedDecoration), viewModel.state.value.decorations)
        assertNull(viewModel.state.value.selectedDecorationId)
        assertTrue(viewModel.state.value.generatedPreview.isEmpty())
    }

    private fun createViewModel(
        apiRepository: StickerApiRepository = mockk {
            coEvery { improve(any()) } returns emptyList()
        }
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
            onDeviceImageProcessor = onDeviceImageProcessor
        )
    }
}
