package presentation.createpack

import domain.model.Sticker
import domain.model.StickerPack
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatePackDraftMapperTest {

    @Test
    fun stickerMapsToDraftUsingSourceImageForStatic() {
        val sticker = Sticker(
            imageFile = "/preview.webp",
            sourceImageFile = "/base.webp",
            emojis = listOf("⭐"),
        )

        val draft = sticker.toDraftSticker()

        assertEquals("/base.webp", draft.imagePath)
        assertEquals(false, draft.isAnimated)
    }

    @Test
    fun mergeDraftStickersKeepsPersistedAndUnsavedSessionDrafts() {
        val persisted = listOf(DraftSticker(imagePath = "/saved/a.webp"))
        val inSession = listOf(
            DraftSticker(imagePath = "/saved/a.webp"),
            DraftSticker(imagePath = "/tmp/new.webp"),
        )

        val merged = mergeDraftStickersFromPack(persisted, inSession)

        assertEquals(2, merged.size)
        assertEquals("/saved/a.webp", merged[0].imagePath)
        assertEquals("/tmp/new.webp", merged[1].imagePath)
    }

    @Test
    fun stickerPackMapsAllStickersToDrafts() {
        val pack = StickerPack(
            identifier = "pack-1",
            name = "Pack",
            publisher = "Setiker",
            trayImageFile = "tray.webp",
            stickers = listOf(
                Sticker(imageFile = "/preview.webp", sourceImageFile = "/base.webp", emojis = listOf("⭐")),
            ),
        )

        assertEquals(1, pack.toDraftStickers().size)
        assertEquals("/base.webp", pack.toDraftStickers().first().imagePath)
    }
}
