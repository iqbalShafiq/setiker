package presentation.editor

import domain.model.DecorationFont
import domain.model.TextDecoration
import presentation.createpack.DraftSticker
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorGeneratedDraftStateTest {
    @Test
    fun generatedPreviewAndApplyIntentStoreDraftStickerWithDecorations() {
        val decoration = TextDecoration(
            id = "api-text-1",
            text = "Hello",
            font = DecorationFont.Sans
        )
        val draft = DraftSticker(
            imagePath = "/local/generated.png",
            decorations = listOf(decoration)
        )

        val state = EditorState(generatedPreview = listOf(draft))
        val intent = EditorIntent.ApplyGeneratedSticker(draft)

        assertEquals(draft, state.generatedPreview.single())
        assertEquals(draft, intent.draft)
    }

    @Test
    fun improveStickerIntentExistsForEditorAiImproveAction() {
        val intent: EditorIntent = EditorIntent.RequestImproveSticker

        assertEquals(EditorIntent.RequestImproveSticker, intent)
    }
}
