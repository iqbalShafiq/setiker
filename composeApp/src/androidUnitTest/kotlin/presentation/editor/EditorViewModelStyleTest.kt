package presentation.editor

import data.remote.StickerApiRepository
import data.storage.StickerFileStorage
import data.util.EmojiPreferences
import data.util.OnDeviceImageProcessor
import domain.model.DecorationFont
import domain.model.TextDecoration
import domain.model.TextDecorationStyle
import domain.model.TextDecorationStyleRegistry
import domain.repository.AiQuotaRepository
import domain.repository.StickerRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import presentation.aijob.ViewModelAiJobTestSupport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelStyleTest {
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
    fun addTextDecorationAppliesSelectedStylePreset() = runTest {
        val viewModel = createViewModel()
        val preset = TextDecorationStyleRegistry.preset(TextDecorationStyle.StickerPop)

        viewModel.onIntent(EditorIntent.AddTextDecoration("Hello", TextDecorationStyle.StickerPop))
        advanceUntilIdle()

        val decoration = viewModel.state.value.decorations.single() as TextDecoration
        assertEquals(TextDecorationStyle.StickerPop, decoration.style)
        assertEquals(preset.font, decoration.font)
        assertEquals(preset.fontWeight, decoration.fontWeight)
        assertEquals(preset.defaultTextColorArgb, decoration.textColorArgb)
        assertNotNull(viewModel.state.value.selectedDecorationId)
    }

    @Test
    fun updateTextDecorationStylePreservesFillColor() = runTest {
        val viewModel = createViewModel()
        val customFill = 0xFFFF5722L

        viewModel.onIntent(EditorIntent.AddTextDecoration("Hi", TextDecorationStyle.ClassicOutline))
        advanceUntilIdle()
        val id = viewModel.state.value.selectedDecorationId!!

        viewModel.onIntent(EditorIntent.UpdateTextDecorationColor(id, customFill))
        viewModel.onIntent(EditorIntent.UpdateTextDecorationStyle(id, TextDecorationStyle.BubbleRed))
        advanceUntilIdle()

        val decoration = viewModel.state.value.decorations.single() as TextDecoration
        assertEquals(TextDecorationStyle.BubbleRed, decoration.style)
        assertEquals(customFill, decoration.textColorArgb)
        assertEquals(
            TextDecorationStyleRegistry.preset(TextDecorationStyle.BubbleRed).font,
            decoration.font
        )
    }

    @Test
    fun updateTextDecorationFontSwitchesToCustomStyle() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(EditorIntent.AddTextDecoration("Hi", TextDecorationStyle.NeonCyan))
        advanceUntilIdle()
        val id = viewModel.state.value.selectedDecorationId!!

        viewModel.onIntent(EditorIntent.UpdateTextDecorationFont(id, DecorationFont.Mono))
        advanceUntilIdle()

        val decoration = viewModel.state.value.decorations.single() as TextDecoration
        assertEquals(TextDecorationStyle.Custom, decoration.style)
        assertEquals(DecorationFont.Mono, decoration.font)
    }

    private fun createViewModel(): EditorViewModel {
        val deps = ViewModelAiJobTestSupport.editorDependencies()
        return EditorViewModel(
            repository = mockk<StickerRepository>(relaxed = true),
            emojiPreferences = mockk<EmojiPreferences>(relaxed = true),
            fileStorage = mockk<StickerFileStorage>(relaxed = true),
            apiRepository = mockk<StickerApiRepository>(relaxed = true),
            onDeviceImageProcessor = mockk<OnDeviceImageProcessor>(relaxed = true),
            aiJobManager = deps.manager,
            enqueueHelper = deps.enqueueHelper,
            draftResultApplier = deps.draftResultApplier,
            aiQuotaRepository = mockk<AiQuotaRepository>(relaxed = true)
        )
    }
}
