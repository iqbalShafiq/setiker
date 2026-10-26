package data.storage.webp

/**
 * Reads and writes the WebP RIFF container described by the WebP specification
 * (<https://developers.google.com/speed/webp/docs/riff_container>).
 *
 * Android has no API that encodes an *animation*. `Bitmap.compress` produces a still WebP at
 * best (a `VP8 ` or `VP8L` chunk), and the only animated encoder on the platform is libwebp
 * reached through JNI. This file closes that gap without touching native code: it takes the
 * still WebP that `Bitmap.compress` already produces and re-wraps those frames into the
 * animation container (`VP8X` + `ANIM` + one `ANMF` per frame).
 *
 * Dropping the native encoder is the point. It removes the app's only `.so` dependency, and
 * with it the 16 KB page-size problem that came with it: there is nothing left to load at
 * runtime, and the encoder works the same on every ABI.
 *
 * The layout this writes was verified against a reference file produced by libwebp's own
 * muxer, and the output is readable by libwebp's demuxer (the decoder under
 * `FBWebPImage`/`ImageDecoder`), which is what WhatsApp validates against.
 */
internal object WebpContainer {

    /** A still image that becomes one frame of the animation. */
    /**
     * The rectangle of the canvas a frame covers.
     *
     * A frame that only changes part of the picture stores just that rectangle: the rest of the
     * canvas keeps showing the previous frame, which is what keeps a long animation small
     * enough for WhatsApp's 500 KB limit.
     */
    class Rect(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    )

    class Frame(
        /** A complete still WebP file, as produced by `Bitmap.compress`. */
        val stillImage: ByteArray,
        /** How long the frame stays on screen, in milliseconds. */
        val durationMs: Int,
        /** The part of the canvas this frame covers; `null` means the whole canvas. */
        val rect: Rect? = null
    )

    /** The animation flag of the `VP8X` chunk. */
    private const val FLAG_ANIMATION = 0x02

    /** The alpha flag of the `VP8X` chunk: set when at least one frame carries an `ALPH` chunk. */
    private const val FLAG_ALPHA = 0x10

    /**
     * The blend and dispose bits of an `ANMF` chunk, `0b000000_1_0`. Bit 1 (`B`) is set, which
     * means "do not blend": the frame overwrites its rectangle instead of composing onto the
     * previous one. That keeps a transparent pixel transparent instead of letting an earlier
     * frame show through, and it is what makes a partial frame safe — whatever the rectangle
     * does not cover keeps showing the previous frame.
     * Bit 0 (`D`) stays clear: there is nothing to dispose of once the frame is drawn.
     */
    private const val ANMF_BLEND_OVERWRITE = 0x02

    private const val CHUNK_HEADER_SIZE = 8

    /** `ANMF` frame header: x, y, width, height, duration and the blend/dispose byte. */
    private const val ANMF_HEADER_SIZE = 16

    private const val RIFF_HEADER_SIZE = 12

    /** `durationMs` is a 24-bit field in the container. */
    private const val MAX_DURATION_MS = 0xFFFFFF

    /** Canvas dimensions are stored as a 24-bit "minus one". */
    private const val MAX_CANVAS_EDGE = 0xFFFFFF

    /**
     * Builds an animated WebP from [frames].
     *
     * Every frame must be a still WebP of exactly the size of its rectangle; the writer checks
     * that as it parses each frame, so a frame that was resized by mistake fails here instead
     * of producing an animation that renders wrong.
     *
     * @param loopCount how many times the animation plays; `0` means forever, which is what a
     *   sticker wants.
     */
    fun animate(
        canvasWidth: Int,
        canvasHeight: Int,
        frames: List<Frame>,
        loopCount: Int = 0
    ): ByteArray {
        require(frames.isNotEmpty()) { "An animation needs at least one frame" }
        require(canvasWidth in 1..MAX_CANVAS_EDGE) { "Invalid canvas width: $canvasWidth" }
        require(canvasHeight in 1..MAX_CANVAS_EDGE) { "Invalid canvas height: $canvasHeight" }
        require(loopCount in 0..0xFFFF) { "Invalid loop count: $loopCount" }

        val fullCanvas = Rect(0, 0, canvasWidth, canvasHeight)
        val rects = frames.map { it.rect ?: fullCanvas }
        val images = frames.mapIndexed { index, frame ->
            val rect = rects[index]
            validateRect(rect, canvasWidth, canvasHeight, index)
            readStillImage(frame.stillImage, index).also { image ->
                require(image.width == rect.width && image.height == rect.height) {
                    "Frame $index is ${image.width}x${image.height}, expected " +
                        "${rect.width}x${rect.height}"
                }
            }
        }

        val hasAlpha = images.any { it.hasAlpha }
        val sink = ChunkSink(frames.sumOf { it.stillImage.size } + 64)

        sink.ascii("RIFF")
        // Patched once the total size is known: the RIFF size field covers everything
        // after itself, so it is "bytes written - 8".
        sink.u32(0)
        sink.ascii("WEBP")

        sink.ascii("VP8X")
        sink.u32(10)
        sink.u8(FLAG_ANIMATION or if (hasAlpha) FLAG_ALPHA else 0)
        sink.u8(0)
        sink.u8(0)
        sink.u8(0)
        sink.u24(canvasWidth - 1)
        sink.u24(canvasHeight - 1)

        sink.ascii("ANIM")
        sink.u32(6)
        // Transparent background, in the [B, G, R, A] byte order the spec asks for.
        sink.u32(0)
        sink.u16(loopCount)

        frames.forEachIndexed { index, frame ->
            val image = images[index]
            val rect = rects[index]
            val alphaSize = image.alpha?.let { CHUNK_HEADER_SIZE + paddedSize(it.size) } ?: 0
            val bitstreamSize = CHUNK_HEADER_SIZE + paddedSize(image.bitstream.size)

            sink.ascii("ANMF")
            sink.u32(ANMF_HEADER_SIZE + alphaSize + bitstreamSize)
            // Frame X and Y are stored in units of two pixels, hence the even left/top.
            sink.u24(rect.left / 2)
            sink.u24(rect.top / 2)
            sink.u24(rect.width - 1)
            sink.u24(rect.height - 1)
            sink.u24(frame.durationMs.coerceIn(0, MAX_DURATION_MS))
            sink.u8(ANMF_BLEND_OVERWRITE)

            // Frame data: the optional alpha chunk first, then the bitstream, each padded to an
            // even size. The padding is part of the chunk stream, but not of the chunk size.
            image.alpha?.let { alpha ->
                sink.ascii("ALPH")
                sink.u32(alpha.size)
                sink.bytes(alpha)
                sink.padToEven()
            }
            sink.ascii(image.fourCc)
            sink.u32(image.bitstream.size)
            sink.bytes(image.bitstream)
            sink.padToEven()
        }

        val bytes = sink.toByteArray()
        writeU32(bytes, 4, bytes.size - 8)
        return bytes
    }

    /**
     * The number of `ANMF` chunks in [bytes], or `0` when [bytes] is not a WebP file at all.
     *
     * This reads the container only; it does not decode anything. It exists so the encoder can
     * assert its own output before handing a file to WhatsApp.
     */
    fun frameCount(bytes: ByteArray): Int {
        if (!hasRiffHeader(bytes)) return 0
        var offset = RIFF_HEADER_SIZE
        var count = 0
        while (offset + CHUNK_HEADER_SIZE <= bytes.size) {
            val size = readU32(bytes, offset + 4)
            if (size < 0 || offset + CHUNK_HEADER_SIZE + size > bytes.size) return count
            if (matches(bytes, offset, "ANMF")) count++
            offset += CHUNK_HEADER_SIZE + paddedSize(size)
        }
        return count
    }

    /** A still WebP taken apart: the alpha plane (when present) and the image bitstream. */
    private class StillImage(
        val fourCc: String,
        val bitstream: ByteArray,
        val alpha: ByteArray?,
        val width: Int,
        val height: Int,
        /**
         * Whether the frame carries transparency. A lossy frame carries it in a separate
         * `ALPH` chunk; a lossless (`VP8L`) frame carries it inside the bitstream, which is
         * why the bitstream header has to be read as well.
         */
        val hasAlpha: Boolean
    )

    private class FrameGeometry(val width: Int, val height: Int, val alphaHint: Boolean)

    /**
     * Walks the chunks of a still WebP and returns the two that make up an `ANMF` frame.
     *
     * Both layouts `Bitmap.compress` can produce are handled: the simple one (`VP8 `/`VP8L`
     * only, used when the image is opaque) and the extended one (`VP8X`, an optional `ALPH`,
     * then the bitstream, used when there is transparency). Metadata chunks such as `ICCP`,
     * `EXIF` or `XMP ` are dropped: an animated sticker has no use for them.
     */
    private fun readStillImage(bytes: ByteArray, index: Int): StillImage {
        require(bytes.size > RIFF_HEADER_SIZE && hasRiffHeader(bytes)) {
            "Frame $index is not a WebP file"
        }

        var offset = RIFF_HEADER_SIZE
        var alpha: ByteArray? = null
        var fourCc: String? = null
        var bitstream: ByteArray? = null

        while (offset + CHUNK_HEADER_SIZE <= bytes.size) {
            val size = readU32(bytes, offset + 4)
            val payloadStart = offset + CHUNK_HEADER_SIZE
            require(size >= 0 && payloadStart + size <= bytes.size) {
                "Frame $index has a corrupt chunk at offset $offset"
            }
            val chunk = fourCc(bytes, offset)
            when (chunk) {
                "ALPH" -> alpha = bytes.copyOfRange(payloadStart, payloadStart + size)
                "VP8 ", "VP8L" -> {
                    fourCc = chunk
                    bitstream = bytes.copyOfRange(payloadStart, payloadStart + size)
                }
            }
            if (bitstream != null) break
            offset += CHUNK_HEADER_SIZE + paddedSize(size)
        }

        val resolvedFourCc = requireNotNull(fourCc) { "Frame $index has no VP8/VP8L chunk" }
        val resolvedBitstream = requireNotNull(bitstream)
        val geometry = frameGeometry(resolvedFourCc, resolvedBitstream, index)
        return StillImage(
            fourCc = resolvedFourCc,
            bitstream = resolvedBitstream,
            alpha = alpha,
            width = geometry.width,
            height = geometry.height,
            hasAlpha = alpha != null || geometry.alphaHint
        )
    }

    /**
     * Reads the pixel size (and, for a lossless frame, the transparency hint) out of a `VP8 `
     * or `VP8L` bitstream header, so a frame that does not match the canvas is caught before it
     * reaches the file.
     */
    private fun frameGeometry(fourCc: String, bitstream: ByteArray, index: Int): FrameGeometry {
        if (fourCc == "VP8L") {
            // Signature byte, then a 32-bit little-endian word: 14 bits of width - 1,
            // 14 bits of height - 1, an alpha hint and 3 version bits.
            require(bitstream.size >= 5 && bitstream[0].toInt() and 0xFF == 0x2F) {
                "Frame $index has a malformed VP8L header"
            }
            val packed = readU32(bitstream, 1)
            return FrameGeometry(
                width = (packed and 0x3FFF) + 1,
                height = ((packed ushr 14) and 0x3FFF) + 1,
                alphaHint = ((packed ushr 28) and 1) == 1
            )
        }

        // VP8: a 3-byte frame tag, the 0x9D 0x01 0x2A start code, then 16 bits of width and
        // 16 bits of height (14 significant bits each).
        require(bitstream.size >= 10) { "Frame $index has a malformed VP8 header" }
        val hasStartCode = bitstream[3].toInt() and 0xFF == 0x9D &&
            bitstream[4].toInt() and 0xFF == 0x01 &&
            bitstream[5].toInt() and 0xFF == 0x2A
        require(hasStartCode) { "Frame $index has a malformed VP8 header" }
        val width = (bitstream[6].toInt() and 0xFF) or ((bitstream[7].toInt() and 0xFF) shl 8)
        val height = (bitstream[8].toInt() and 0xFF) or ((bitstream[9].toInt() and 0xFF) shl 8)
        return FrameGeometry(
            width = width and 0x3FFF,
            height = height and 0x3FFF,
            alphaHint = false
        )
    }

    private fun validateRect(rect: Rect, canvasWidth: Int, canvasHeight: Int, index: Int) {
        require(rect.width > 0 && rect.height > 0) {
            "Frame $index has an empty rectangle: ${rect.width}x${rect.height}"
        }
        require(rect.left >= 0 && rect.top >= 0) {
            "Frame $index starts outside the canvas: ${rect.left},${rect.top}"
        }
        require(rect.left % 2 == 0 && rect.top % 2 == 0) {
            "Frame $index must start on an even coordinate, was ${rect.left},${rect.top}"
        }
        require(rect.left + rect.width <= canvasWidth && rect.top + rect.height <= canvasHeight) {
            "Frame $index (${rect.left},${rect.top} ${rect.width}x${rect.height}) does not " +
                "fit a ${canvasWidth}x$canvasHeight canvas"
        }
    }

    private fun hasRiffHeader(bytes: ByteArray): Boolean =
        bytes.size > RIFF_HEADER_SIZE &&
            matches(bytes, 0, "RIFF") &&
            matches(bytes, 8, "WEBP")

    private fun matches(bytes: ByteArray, offset: Int, expected: String): Boolean =
        bytes.size >= offset + 4 && fourCc(bytes, offset) == expected

    private fun fourCc(bytes: ByteArray, offset: Int): String =
        buildString(4) {
            for (i in 0 until 4) append((bytes[offset + i].toInt() and 0xFF).toChar())
        }

    private fun readU32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun writeU32(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    /** RIFF chunks are padded to an even number of bytes; the pad byte is not part of the size. */
    private fun paddedSize(size: Int): Int = size + (size and 1)

    /** A growable little-endian byte buffer, so this file stays free of `java.io`. */
    private class ChunkSink(capacity: Int) {
        private var buffer = ByteArray(capacity.coerceAtLeast(64))
        private var size = 0

        fun u8(value: Int) {
            ensure(1)
            buffer[size++] = value.toByte()
        }

        fun u16(value: Int) {
            ensure(2)
            buffer[size++] = (value and 0xFF).toByte()
            buffer[size++] = ((value ushr 8) and 0xFF).toByte()
        }

        fun u24(value: Int) {
            ensure(3)
            buffer[size++] = (value and 0xFF).toByte()
            buffer[size++] = ((value ushr 8) and 0xFF).toByte()
            buffer[size++] = ((value ushr 16) and 0xFF).toByte()
        }

        fun u32(value: Int) {
            ensure(4)
            buffer[size++] = (value and 0xFF).toByte()
            buffer[size++] = ((value ushr 8) and 0xFF).toByte()
            buffer[size++] = ((value ushr 16) and 0xFF).toByte()
            buffer[size++] = ((value ushr 24) and 0xFF).toByte()
        }

        fun ascii(value: String) {
            ensure(value.length)
            value.forEach { buffer[size++] = it.code.toByte() }
        }

        fun bytes(value: ByteArray) {
            ensure(value.size)
            value.copyInto(buffer, size)
            size += value.size
        }

        fun padToEven() {
            if (size and 1 == 1) u8(0)
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)

        private fun ensure(extra: Int) {
            if (size + extra <= buffer.size) return
            var capacity = buffer.size
            while (capacity < size + extra) capacity *= 2
            buffer = buffer.copyOf(capacity)
        }
    }
}
