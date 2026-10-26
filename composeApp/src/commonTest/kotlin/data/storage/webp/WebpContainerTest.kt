package data.storage.webp

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The container format is the contract with every WebP decoder out there — WhatsApp's native
 * reader, Android's `ImageDecoder`, browsers — so these tests read the bytes back and assert
 * the layout the specification describes, rather than trusting the writer.
 */
class WebpContainerTest {

    private val canvas = 512

    @Test
    fun theFileStartsWithTheRiffHeaderAndTheAnimationChunks() {
        val animated = WebpContainer.animate(
            canvasWidth = canvas,
            canvasHeight = canvas,
            frames = listOf(frame(0, 100), frame(1, 100))
        )

        assertEquals("RIFF", fourCc(animated, 0))
        assertEquals(animated.size - 8, u32(animated, 4))
        assertEquals("WEBP", fourCc(animated, 8))

        val chunks = chunks(animated)
        assertContentEquals(
            listOf("VP8X", "ANIM", "ANMF", "ANMF"),
            chunks.map { it.fourCc }
        )
    }

    @Test
    fun theVp8xChunkDescribesTheCanvasAsMinusOne() {
        val animated = WebpContainer.animate(canvas, canvas, listOf(frame(0, 100)))

        val vp8x = chunks(animated).first { it.fourCc == "VP8X" }
        assertEquals(10, vp8x.size)
        // Animation on, alpha off, then three reserved bytes.
        assertEquals(0x02, vp8x.payload(animated)[0].toInt() and 0xFF)
        assertEquals(0, vp8x.payload(animated)[1].toInt())
        assertEquals(0, vp8x.payload(animated)[2].toInt())
        assertEquals(0, vp8x.payload(animated)[3].toInt())
        // Canvas size is stored as "minus one", after the flag and the reserved bytes.
        assertEquals(canvas - 1, u24(animated, vp8x.payloadOffset + 4))
        assertEquals(canvas - 1, u24(animated, vp8x.payloadOffset + 7))
    }

    @Test
    fun theAlphaFlagFollowsTheFrames() {
        val opaque = WebpContainer.animate(canvas, canvas, listOf(frame(0, 100)))
        assertEquals(0x02, chunks(opaque).first { it.fourCc == "VP8X" }
            .payload(opaque)[0].toInt() and 0xFF)

        val transparent = WebpContainer.animate(
            canvas,
            canvas,
            listOf(frame(0, 100, withAlpha = true))
        )
        assertEquals(0x12, chunks(transparent).first { it.fourCc == "VP8X" }
            .payload(transparent)[0].toInt() and 0xFF)
    }

    @Test
    fun theAnimChunkLoopsForeverOnATransparentBackground() {
        val animated = WebpContainer.animate(canvas, canvas, listOf(frame(0, 100)))

        val anim = chunks(animated).first { it.fourCc == "ANIM" }
        assertEquals(6, anim.size)
        // Background colour is [B, G, R, A]; transparent black so no white shows through.
        assertEquals(0, u32(animated, anim.payloadOffset))
        assertEquals(0, u16(animated, anim.payloadOffset + 4))
    }

    @Test
    fun eachFrameCarriesItsDurationAndOverwritesTheCanvas() {
        val animated = WebpContainer.animate(
            canvas,
            canvas,
            listOf(frame(0, 100), frame(1, 250), frame(2, 8))
        )

        val frames = chunks(animated).filter { it.fourCc == "ANMF" }
        assertEquals(3, frames.size)
        assertContentEquals(listOf(100, 250, 8), frames.map { u24(animated, it.payloadOffset + 12) })
        frames.forEach { anmf ->
            assertEquals(0, u24(animated, anmf.payloadOffset)) // x
            assertEquals(0, u24(animated, anmf.payloadOffset + 3)) // y
            assertEquals(canvas - 1, u24(animated, anmf.payloadOffset + 6))
            assertEquals(canvas - 1, u24(animated, anmf.payloadOffset + 9))
            // Blend is off (bit 1) so a transparent pixel stays transparent instead of
            // letting the previous frame show through; dispose stays off (bit 0).
            assertEquals(0x02, animated[anmf.payloadOffset + 15].toInt() and 0xFF)
        }
    }

    @Test
    fun theFrameDataHoldsAlphaFirstThenTheBitstream() {
        val animated = WebpContainer.animate(
            canvas,
            canvas,
            listOf(frame(0, 100, withAlpha = true))
        )

        val anmf = chunks(animated).first { it.fourCc == "ANMF" }
        val frameChunks = chunks(animated, anmf.payloadOffset + 16, anmf.payloadOffset + anmf.size)
        assertContentEquals(listOf("ALPH", "VP8 "), frameChunks.map { it.fourCc })
        // The still image's own VP8X chunk is metadata about a canvas that no longer exists
        // and must not be copied into a frame.
        assertTrue(frameChunks.none { it.fourCc == "VP8X" })
    }

    @Test
    fun oddSizedChunksArePaddedWithoutGrowingTheChunkSize() {
        // A 1-byte alpha plane and a 7-byte bitstream are both odd, so both need a pad byte.
        val annotated = WebpContainer.animate(
            canvas,
            canvas,
            listOf(frame(0, 100, withAlpha = true, alphaSize = 1, bitstreamTail = 7))
        )

        assertEquals(0, annotated.size % 2)
        assertEquals(annotated.size - 8, u32(annotated, 4))

        val anmf = chunks(annotated).first { it.fourCc == "ANMF" }
        val frameChunks = chunks(annotated, anmf.payloadOffset + 16, anmf.payloadOffset + anmf.size)
        val alpha = frameChunks.first { it.fourCc == "ALPH" }
        val bitstream = frameChunks.first { it.fourCc == "VP8 " }
        assertEquals(1, alpha.size)
        assertEquals(17, bitstream.size) // 10 header bytes + 7 filler bytes
        // Frame header, then each sub-chunk with its pad: ALPH (8 + 1 + 1) and
        // VP8 (8 + 17 + 1).
        assertEquals(16 + (8 + 2) + (8 + 18), anmf.size)
    }

    @Test
    fun theContainerKeepsEveryFrameItIsGiven() {
        val animated = WebpContainer.animate(
            canvas,
            canvas,
            List(5) { frame(it, 100) }
        )

        // The old libwebp path could silently drop a frame that looked like the previous
        // one; the writer here cannot.
        assertEquals(5, WebpContainer.frameCount(animated))
    }

    @Test
    fun aFrameThatDoesNotMatchTheCanvasIsRejected() {
        val error = assertFailsWith<IllegalArgumentException> {
            WebpContainer.animate(
                canvasWidth = canvas,
                canvasHeight = canvas,
                frames = listOf(frame(0, 100, width = 256, height = 256))
            )
        }
        assertTrue(error.message.orEmpty().contains("256x256"))
    }

    @Test
    fun aPartialFrameStoresItsRectangleInHalves() {
        val rect = WebpContainer.Rect(left = 4, top = 6, width = 100, height = 50)
        val animated = WebpContainer.animate(
            canvas,
            canvas,
            listOf(frame(0, 100, width = 100, height = 50, rect = rect))
        )

        val anmf = chunks(animated).first { it.fourCc == "ANMF" }
        // X and Y are stored divided by two, so the rectangle has to start on an even pixel.
        assertEquals(2, u24(animated, anmf.payloadOffset))
        assertEquals(3, u24(animated, anmf.payloadOffset + 3))
        assertEquals(99, u24(animated, anmf.payloadOffset + 6))
        assertEquals(49, u24(animated, anmf.payloadOffset + 9))
        // The canvas still reports the full sticker size.
        val vp8x = chunks(animated).first { it.fourCc == "VP8X" }
        assertEquals(canvas - 1, u24(animated, vp8x.payloadOffset + 4))
    }

    @Test
    fun aPartialFrameKeepsTheCanvasRectangleAvailableForTheNextFrame() {
        val animated = WebpContainer.animate(
            canvas,
            canvas,
            listOf(
                frame(0, 100),
                frame(1, 100, width = 64, height = 64, rect = WebpContainer.Rect(0, 0, 64, 64))
            )
        )

        val frames = chunks(animated).filter { it.fourCc == "ANMF" }
        assertEquals(511, u24(animated, frames[0].payloadOffset + 6))
        assertEquals(63, u24(animated, frames[1].payloadOffset + 6))
    }

    @Test
    fun aRectangleOutsideTheCanvasIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            WebpContainer.animate(
                canvas,
                canvas,
                listOf(
                    frame(
                        0,
                        100,
                        width = 100,
                        height = 50,
                        rect = WebpContainer.Rect(500, 0, 100, 50)
                    )
                )
            )
        }
    }

    @Test
    fun aRectangleOnAnOddCoordinateIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            WebpContainer.animate(
                canvas,
                canvas,
                listOf(
                    frame(
                        0,
                        100,
                        width = 10,
                        height = 10,
                        rect = WebpContainer.Rect(3, 0, 10, 10)
                    )
                )
            )
        }
    }

    @Test
    fun aFrameThatDoesNotMatchItsRectangleIsRejected() {
        val error = assertFailsWith<IllegalArgumentException> {
            WebpContainer.animate(
                canvas,
                canvas,
                listOf(
                    frame(
                        0,
                        100,
                        width = 64,
                        height = 64,
                        rect = WebpContainer.Rect(0, 0, 100, 50)
                    )
                )
            )
        }
        assertTrue(error.message.orEmpty().contains("64x64"))
    }

    @Test
    fun somethingThatIsNotAWebpIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            WebpContainer.animate(canvas, canvas, listOf(WebpContainer.Frame(ByteArray(32), 100)))
        }
    }

    @Test
    fun frameCountIsZeroWhenTheFileIsNotRiff() {
        assertEquals(0, WebpContainer.frameCount(ByteArray(4)))
        assertEquals(0, WebpContainer.frameCount("not a webp file".encodeToByteArray()))
    }

    @Test
    fun aLosslessFrameIsAcceptedAndKeepsItsAlphaHint() {
        val animated = WebpContainer.animate(
            canvas,
            canvas,
            listOf(losslessFrame(0, 100, hasAlpha = true))
        )

        assertEquals(1, WebpContainer.frameCount(animated))
        assertEquals(0x12, chunks(animated).first { it.fourCc == "VP8X" }
            .payload(animated)[0].toInt() and 0xFF)
        val anmf = chunks(animated).first { it.fourCc == "ANMF" }
        assertContentEquals(
            listOf("VP8L"),
            chunks(animated, anmf.payloadOffset + 16, anmf.payloadOffset + anmf.size)
                .map { it.fourCc }
        )
    }

    // --- Fixtures -------------------------------------------------------------------------

    private fun frame(
        index: Int,
        durationMs: Int,
        withAlpha: Boolean = false,
        width: Int = canvas,
        height: Int = canvas,
        rect: WebpContainer.Rect? = null,
        alphaSize: Int = 4,
        bitstreamTail: Int = 12
    ): WebpContainer.Frame =
        WebpContainer.Frame(
            stillImage = stillVp8(index, width, height, withAlpha, alphaSize, bitstreamTail),
            durationMs = durationMs,
            rect = rect
        )

    private fun losslessFrame(index: Int, durationMs: Int, hasAlpha: Boolean): WebpContainer.Frame =
        WebpContainer.Frame(stillVp8l(index, hasAlpha), durationMs)

    /**
     * A still WebP shaped like the one `Bitmap.compress` produces: the simple layout when the
     * image is opaque, and `VP8X` + `ALPH` + `VP8 ` when it is not.
     */
    private fun stillVp8(
        index: Int,
        width: Int,
        height: Int,
        withAlpha: Boolean,
        alphaSize: Int,
        bitstreamTail: Int
    ): ByteArray {
        val bitstream = vp8Bitstream(width, height, bitstreamTail, seed = index)
        val alpha = ByteArray(alphaSize) { (it + index).toByte() }
        val chunks = mutableListOf<Pair<String, ByteArray>>()
        if (withAlpha) {
            chunks += "VP8X" to vp8xPayload(hasAlpha = true, width = width, height = height)
            chunks += "ALPH" to alpha
        }
        chunks += "VP8 " to bitstream
        return riff(chunks)
    }

    private fun stillVp8l(index: Int, hasAlpha: Boolean): ByteArray {
        val packed = (canvas - 1) or ((canvas - 1) shl 14) or (if (hasAlpha) 1 shl 28 else 0)
        val payload = byteArrayOf(0x2F) + le32(packed) + ByteArray(8) { (it + index).toByte() }
        return riff(listOf("VP8L" to payload))
    }

    /** A VP8 frame header (frame tag, start code, dimensions) followed by filler bytes. */
    private fun vp8Bitstream(width: Int, height: Int, tail: Int, seed: Int): ByteArray =
        byteArrayOf(0x00, 0x00, 0x00, 0x9D.toByte(), 0x01, 0x2A) +
            le16(width) + le16(height) +
            ByteArray(tail) { (it * 7 + seed).toByte() }

    private fun vp8xPayload(hasAlpha: Boolean, width: Int, height: Int): ByteArray =
        byteArrayOf((if (hasAlpha) 0x10 else 0).toByte(), 0, 0, 0) +
            le24(width - 1) + le24(height - 1)

    private fun riff(chunks: List<Pair<String, ByteArray>>): ByteArray {
        val body = mutableListOf<Byte>()
        chunks.forEach { (fourCc, payload) ->
            body += fourCc.encodeToByteArray().toList()
            body += le32(payload.size).toList()
            body += payload.toList()
            if (payload.size and 1 == 1) body += 0
        }
        val file = mutableListOf<Byte>()
        file += "RIFF".encodeToByteArray().toList()
        file += le32(body.size + 4).toList()
        file += "WEBP".encodeToByteArray().toList()
        file += body
        return file.toByteArray()
    }

    // --- Byte readers ---------------------------------------------------------------------

    private class Chunk(
        val fourCc: String,
        /** Offset of the chunk's 8-byte header. */
        val headerOffset: Int,
        /** Offset of the chunk's payload (just past the header). */
        val payloadOffset: Int,
        /** Size declared by the chunk, which excludes its padding byte. */
        val size: Int
    ) {
        fun payload(bytes: ByteArray): ByteArray =
            bytes.copyOfRange(payloadOffset, payloadOffset + size)
    }

    private fun chunks(bytes: ByteArray, from: Int = 12, until: Int = bytes.size): List<Chunk> {
        val result = mutableListOf<Chunk>()
        var offset = from
        while (offset + 8 <= until) {
            val size = u32(bytes, offset + 4)
            result += Chunk(fourCc(bytes, offset), offset, offset + 8, size)
            offset += 8 + size + (size and 1)
        }
        return result
    }

    private fun fourCc(bytes: ByteArray, offset: Int): String =
        bytes.copyOfRange(offset, offset + 4).decodeToString()

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun u24(bytes: ByteArray, offset: Int): Int =
        u16(bytes, offset) or ((bytes[offset + 2].toInt() and 0xFF) shl 16)

    private fun u32(bytes: ByteArray, offset: Int): Int =
        u24(bytes, offset) or ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun le16(value: Int): ByteArray =
        byteArrayOf((value and 0xFF).toByte(), ((value ushr 8) and 0xFF).toByte())

    private fun le24(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte()
        )

    private fun le32(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte()
        )
}
