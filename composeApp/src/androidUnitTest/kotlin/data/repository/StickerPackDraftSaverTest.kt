package data.repository

import data.storage.StickerFileStorage
import domain.model.StickerDecoration
import domain.model.StickerDraftInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StickerPackDraftSaverTest {

    @Test
    fun savesStaticPackWithBaseAndPreviewFiles() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        val decoration = mockk<StickerDecoration>()
        coEvery { fileStorage.saveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
        coEvery { fileStorage.saveStickerImage("/tmp/a.png", "sticker_pack_0_base.webp") } returns "/saved/a_base.webp"
        coEvery {
            fileStorage.saveStickerImageWithDecorations(
                sourcePath = "/saved/a_base.webp",
                fileName = "sticker_pack_0_preview.webp",
                decorations = listOf(decoration)
            )
        } returns "/saved/a_preview.webp"
        coEvery { fileStorage.saveStickerImage("/tmp/b.png", "sticker_pack_1_base.webp") } returns "/saved/b_base.webp"

        val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })

        val pack = saver.buildDraftPack(
            StickerDraftInput(
                identifier = "pack",
                name = "Pack",
                publisher = "Pub",
                visibility = "PRIVATE",
                trayImagePath = "/tmp/tray.png",
                stickers = listOf(
                    StickerDraftInput.StickerInput(
                        imagePath = "/tmp/a.png",
                        decorations = listOf(decoration)
                    ),
                    StickerDraftInput.StickerInput(imagePath = "/tmp/b.png")
                )
            )
        )

        assertFalse(pack.isAnimated)
        assertEquals("/saved/tray.webp", pack.trayImageFile)
        assertEquals(2, pack.stickers.size)
        assertEquals("/saved/a_preview.webp", pack.stickers[0].imageFile)
        assertEquals("/saved/a_base.webp", pack.stickers[0].sourceImageFile)
        assertEquals(listOf(decoration), pack.stickers[0].decorations)
        assertEquals("/saved/b_base.webp", pack.stickers[1].imageFile)
        assertEquals("/saved/b_base.webp", pack.stickers[1].sourceImageFile)

        coVerify(exactly = 0) { fileStorage.encodeSingleFrameAnimatedWebP(any(), any(), any()) }
    }

    @Test
    fun convertsStaticDraftsWhenPackContainsAnimatedSticker() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        val decoration = mockk<StickerDecoration>()
        coEvery { fileStorage.saveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
        coEvery {
            fileStorage.encodeSingleFrameAnimatedWebP(
                sourcePath = "/tmp/static.png",
                fileName = "sticker_pack_0_anim.webp",
                decorations = listOf(decoration)
            )
        } returns "/saved/static_anim.webp"

        val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })

        val pack = saver.buildDraftPack(
            StickerDraftInput(
                identifier = "pack",
                name = "Pack",
                publisher = "Pub",
                visibility = "PRIVATE",
                trayImagePath = "/tmp/tray.png",
                stickers = listOf(
                    StickerDraftInput.StickerInput(
                        imagePath = "/tmp/static.png",
                        decorations = listOf(decoration)
                    ),
                    StickerDraftInput.StickerInput(
                        imagePath = "/tmp/anim.webp",
                        decorations = listOf(decoration),
                        isAnimated = true,
                        sourceVideoFile = "/tmp/video.mp4",
                        frameDecorations = mapOf(0 to listOf(decoration))
                    )
                )
            )
        )

        assertTrue(pack.isAnimated)
        assertEquals("/saved/static_anim.webp", pack.stickers[0].imageFile)
        assertTrue(pack.stickers[0].isAnimated)
        assertNull(pack.stickers[0].sourceImageFile)
        assertEquals("/tmp/anim.webp", pack.stickers[1].imageFile)
        assertTrue(pack.stickers[1].isAnimated)
        assertEquals("/tmp/video.mp4", pack.stickers[1].sourceVideoFile)
        assertEquals(mapOf(0 to listOf(decoration)), pack.stickers[1].frameDecorations)

        coVerify(exactly = 0) { fileStorage.saveStickerImage(any(), any()) }
        coVerify(exactly = 0) { fileStorage.saveStickerImageWithDecorations(any(), any(), any()) }
    }

    @Test
    fun convertsStaticWithoutDecorationsInAnimatedPack() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.saveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
        coEvery {
            fileStorage.encodeSingleFrameAnimatedWebP(
                sourcePath = "/tmp/plain.png",
                fileName = "sticker_pack_0_anim.webp",
                decorations = emptyList()
            )
        } returns "/saved/plain_anim.webp"

        val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })

        val pack = saver.buildDraftPack(
            StickerDraftInput(
                identifier = "pack",
                name = "Pack",
                publisher = "Pub",
                visibility = "PRIVATE",
                trayImagePath = "/tmp/tray.png",
                stickers = listOf(
                    StickerDraftInput.StickerInput(imagePath = "/tmp/plain.png"),
                    StickerDraftInput.StickerInput(imagePath = "/tmp/anim.webp", isAnimated = true)
                )
            )
        )

        assertTrue(pack.isAnimated)
        assertEquals("/saved/plain_anim.webp", pack.stickers[0].imageFile)
        assertTrue(pack.stickers[0].isAnimated)
        assertTrue(pack.stickers[0].decorations.isEmpty())
        coVerify(exactly = 0) { fileStorage.saveStickerImage(any(), any()) }
    }

    @Test
    fun usesUniqueTrayFileNameAcrossSequentialSaves() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        val trayNames = mutableListOf<String>()
        coEvery { fileStorage.saveTrayImage("/tmp/tray.png", capture(trayNames)) } answers { "/saved/${secondArg<String>()}" }
        coEvery { fileStorage.saveStickerImage("/tmp/a.png", any()) } returns "/saved/a_base.webp"

        var now = 1710000000000L
        val saver = StickerPackDraftSaver(
            fileStorage = fileStorage,
            nowEpochMillis = { now++ }
        )
        val input = StickerDraftInput(
            identifier = "pack",
            name = "Pack",
            publisher = "Pub",
            visibility = "PRIVATE",
            trayImagePath = "/tmp/tray.png",
            stickers = listOf(StickerDraftInput.StickerInput(imagePath = "/tmp/a.png"))
        )

        val first = saver.buildDraftPack(input)
        val second = saver.buildDraftPack(input)

        assertEquals(2, trayNames.size)
        assertTrue(trayNames[0].startsWith("tray_pack_"))
        assertTrue(trayNames[0].endsWith(".png"))
        assertTrue(trayNames[1].startsWith("tray_pack_"))
        assertTrue(trayNames[1].endsWith(".png"))
        assertTrue(trayNames[0] != trayNames[1])
        assertTrue(first.trayImageFile != second.trayImageFile)
    }
}
