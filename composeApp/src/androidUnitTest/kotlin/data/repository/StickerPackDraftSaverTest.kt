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
        coEvery { fileStorage.trySaveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
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
    fun convertsDecoratedStaticDraftToAnimatedWebpWhenPackContainsAnimatedSticker() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        val decoration = mockk<StickerDecoration>()
        coEvery { fileStorage.trySaveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
        coEvery { fileStorage.saveStickerImage("/tmp/static.png", "sticker_pack_0_base.webp") } returns "/saved/static_base.webp"
        coEvery {
            fileStorage.saveStickerImageWithDecorations(
                sourcePath = "/saved/static_base.webp",
                fileName = "sticker_pack_0_preview.webp",
                decorations = listOf(decoration)
            )
        } returns "/saved/static_preview.webp"
        coEvery {
            fileStorage.encodeSingleFrameAnimatedWebP(
                sourcePath = "/saved/static_base.webp",
                fileName = "sticker_pack_0_animated.webp",
                decorations = listOf(decoration)
            )
        } returns "/saved/static_animated.webp"

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
        assertEquals("/saved/static_animated.webp", pack.stickers[0].imageFile)
        assertEquals("/saved/static_base.webp", pack.stickers[0].sourceImageFile)
        assertTrue(pack.stickers[0].isAnimated)
        assertEquals(listOf(decoration), pack.stickers[0].decorations)
        assertEquals("/tmp/anim.webp", pack.stickers[1].imageFile)
        assertTrue(pack.stickers[1].isAnimated)
        assertEquals("/tmp/video.mp4", pack.stickers[1].sourceVideoFile)
        assertEquals(mapOf(0 to listOf(decoration)), pack.stickers[1].frameDecorations)
        coVerify(exactly = 1) {
            fileStorage.encodeSingleFrameAnimatedWebP(
                sourcePath = "/saved/static_base.webp",
                fileName = "sticker_pack_0_animated.webp",
                decorations = listOf(decoration)
            )
        }
    }

    @Test
    fun convertsPlainStaticDraftToAnimatedWebpWhenPackContainsAnimatedSticker() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.trySaveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
        coEvery { fileStorage.saveStickerImage("/tmp/plain.png", "sticker_pack_0_base.webp") } returns "/saved/plain_base.webp"
        coEvery {
            fileStorage.encodeSingleFrameAnimatedWebP(
                sourcePath = "/saved/plain_base.webp",
                fileName = "sticker_pack_0_animated.webp",
                decorations = emptyList()
            )
        } returns "/saved/plain_animated.webp"

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
        assertEquals("/saved/plain_animated.webp", pack.stickers[0].imageFile)
        assertEquals("/saved/plain_base.webp", pack.stickers[0].sourceImageFile)
        assertTrue(pack.stickers[0].isAnimated)
        assertEquals("/tmp/anim.webp", pack.stickers[1].imageFile)
        assertTrue(pack.stickers[1].isAnimated)
        coVerify(exactly = 1) {
            fileStorage.encodeSingleFrameAnimatedWebP(
                sourcePath = "/saved/plain_base.webp",
                fileName = "sticker_pack_0_animated.webp",
                decorations = emptyList()
            )
        }
    }

    @Test
    fun usesUniqueTrayFileNameAcrossSequentialSaves() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        val trayNames = mutableListOf<String>()
        coEvery { fileStorage.trySaveTrayImage("/tmp/tray.png", any()) } answers {
            val trayName = secondArg<String>()
            trayNames += trayName
            "/saved/$trayName"
        }
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

    @Test
    fun leavesTrayEmptyWhenCompressionFails() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.trySaveTrayImage("/tmp/tray.png", any()) } returns null
        coEvery { fileStorage.saveStickerImage("/tmp/a.png", "sticker_pack_0_base.webp") } returns "/saved/a_base.webp"

        val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })
        val pack = saver.buildDraftPack(
            StickerDraftInput(
                identifier = "pack",
                name = "Pack",
                publisher = "Pub",
                visibility = "PRIVATE",
                trayImagePath = "/tmp/tray.png",
                stickers = listOf(StickerDraftInput.StickerInput(imagePath = "/tmp/a.png"))
            )
        )

        assertEquals("", pack.trayImageFile)
    }

    @Test
    fun skipsTraySaveWhenTrayPathBlank() = runTest {
        val fileStorage = mockk<StickerFileStorage>()
        coEvery { fileStorage.saveStickerImage("/tmp/a.png", "sticker_pack_0_base.webp") } returns "/saved/a_base.webp"

        val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })
        val pack = saver.buildDraftPack(
            StickerDraftInput(
                identifier = "pack",
                name = "Pack",
                publisher = "Pub",
                visibility = "PRIVATE",
                trayImagePath = "",
                stickers = listOf(StickerDraftInput.StickerInput(imagePath = "/tmp/a.png"))
            )
        )

        assertEquals("", pack.trayImageFile)
        coVerify(exactly = 0) { fileStorage.trySaveTrayImage(any(), any()) }
    }
}
