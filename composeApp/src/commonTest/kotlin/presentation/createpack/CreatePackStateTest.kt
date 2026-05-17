package presentation.createpack

import domain.model.DecorationFont
import domain.model.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatePackStateTest {
    @Test
    fun generatedPreviewStoresDraftStickersWithDecorations() {
        val decoration = TextDecoration(
            id = "api-text-1",
            text = "Hello",
            font = DecorationFont.Sans
        )
        val draft = DraftSticker(
            imagePath = "/local/generated.png",
            decorations = listOf(decoration)
        )

        val state = CreatePackState(generatedPreview = listOf(draft))

        assertEquals(draft, state.generatedPreview.single())
    }
}
