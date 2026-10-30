package com.setiker.app.storage

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import data.storage.StickerFileStorage
import domain.model.DecodedFrame
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real encoder on a real phone.
 *
 * The host tests check the container bytes; this checks that the file Android itself produced is
 * accepted by the platform decoder, which is the same libwebp demuxer WhatsApp validates with.
 * It is the closest thing to "add it to WhatsApp" that can run without WhatsApp.
 */
@RunWith(AndroidJUnit4::class)
class AnimatedWebpDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theEncoderWritesAnAnimationThePlatformDecoderAccepts() = runBlocking {
        val storage = StickerFileStorage(context)
        val frames = (0 until 3).map { index ->
            DecodedFrame(pngFrame(index), 120L)
        }

        val path = storage.saveAnimatedStickerImage(frames, "device_animated.webp")
        val file = File(path)

        assertTrue("the sticker was not written", file.isFile)
        assertTrue(
            "animated sticker is ${file.length()} bytes, over WhatsApp's 500 KB",
            file.length() in 1..(500 * 1024)
        )

        val bytes = file.readBytes()
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WEBP", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals("the file should hold exactly the three frames", 3, anmfCount(bytes))

        // The platform decoder, which is libwebp under the hood.
        val source = ImageDecoder.createSource(file)
        val drawable = ImageDecoder.decodeDrawable(source)
        assertTrue(
            "Android did not recognise the file as an animation",
            drawable is AnimatedImageDrawable
        )
        drawable as AnimatedImageDrawable
        assertEquals(512, drawable.intrinsicWidth)
        assertEquals(512, drawable.intrinsicHeight)
        // Loop count 0 means "forever", which the platform reports as REPEAT_INFINITE.
        assertEquals(AnimatedImageDrawable.REPEAT_INFINITE, drawable.repeatCount)

        // The first frame must still be the picture we put in, not a blank canvas. Sample the
        // middle of the block: the pixels on its border sit next to transparent ones and the
        // encoder blends chroma there, which is expected and not what this test is about.
        val firstFrame = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
        }
        assertEquals(512, firstFrame.width)
        assertEquals(512, firstFrame.height)
        assertColorClose(FRAME_COLORS[0], firstFrame.getPixel(266, 266))

        copyForInspection(file)
        firstFrame.recycle()
    }

    @Test
    fun aStaticStickerBecomesATwoFrameAnimation() = runBlocking {
        val storage = StickerFileStorage(context)
        val source = File(context.cacheDir, "device_static_source.png")
        source.writeBytes(pngFrame(7))

        val path = storage.encodeSingleFrameAnimatedWebP(source.absolutePath, "device_single.webp")
        val file = File(path)

        assertTrue("the sticker was not written", file.isFile)
        assertEquals(
            "WhatsApp refuses an animated pack whose sticker has one frame",
            2,
            anmfCount(file.readBytes())
        )

        val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file))
        assertTrue(drawable is AnimatedImageDrawable)
        assertEquals(512, (drawable as AnimatedImageDrawable).intrinsicWidth)
        copyForInspection(file)
    }

    // --- Helpers ------------------------------------------------------------------------------

    /** A 512x512 PNG with a solid marker in the middle, different for every frame. */
    private fun pngFrame(index: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val color = FRAME_COLORS[index % FRAME_COLORS.size]
        for (y in 160 until 352 step 24) {
            for (x in 160 until 352 step 24) {
                for (dy in 0 until 20) {
                    for (dx in 0 until 20) {
                        bitmap.setPixel(x + dx, y + dy, color)
                    }
                }
            }
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    /** Counts the top-level `ANMF` chunks, the same walk the container writer does. */
    private fun anmfCount(bytes: ByteArray): Int {
        var offset = 12
        var count = 0
        while (offset + 8 <= bytes.size) {
            val fourCc = String(bytes, offset, 4, Charsets.US_ASCII)
            val size = (bytes[offset + 4].toInt() and 0xFF) or
                ((bytes[offset + 5].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 6].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 7].toInt() and 0xFF) shl 24)
            if (fourCc == "ANMF") count++
            offset += 8 + size + (size and 1)
        }
        return count
    }

    /** Assert that [actual] is [expected] within a few levels per channel, opaque. */
    private fun assertColorClose(expected: Int, actual: Int) {
        val tolerance = 12
        val dr = abs(Color.red(expected) - Color.red(actual))
        val dg = abs(Color.green(expected) - Color.green(actual))
        val db = abs(Color.blue(expected) - Color.blue(actual))
        val da = abs(Color.alpha(expected) - Color.alpha(actual))
        assertTrue(
            "expected ${Integer.toHexString(expected)} but decoded " +
                "${Integer.toHexString(actual)} (at 256,256)",
            dr <= tolerance && dg <= tolerance && db <= tolerance && da <= tolerance
        )
    }

    /** Leaves a copy where `adb pull` can reach it, so the file can be decoded off-device too. */
    private fun copyForInspection(file: File) {
        val dir = context.getExternalFilesDir("webp-check") ?: return
        dir.mkdirs()
        file.copyTo(File(dir, file.name), overwrite = true)
    }

    private companion object {
        val FRAME_COLORS = intArrayOf(
            Color.rgb(240, 60, 60),
            Color.rgb(60, 180, 240),
            Color.rgb(120, 220, 120)
        )
    }
}
