package domain.util

import domain.model.AnimatedStickerSpec
import domain.model.StickerPack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimatedStickerValidatorTest {

    @Test
    fun frameDurationBelow8MsFails() {
        val result = AnimatedStickerValidator.validateFrameDuration(StickerPack.MIN_FRAME_DURATION_MS - 1)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.FrameDurationTooShort, result.reason)
    }

    @Test
    fun frameDurationAtMinIsOk() {
        val result = AnimatedStickerValidator.validateFrameDuration(StickerPack.MIN_FRAME_DURATION_MS)
        assertEquals(AnimatedStickerValidator.Result.Ok, result)
    }

    @Test
    fun totalDurationOver10sFails() {
        val result = AnimatedStickerValidator.validateTotalDuration(StickerPack.MAX_ANIMATION_DURATION_MS + 1)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.TotalDurationTooLong, result.reason)
    }

    @Test
    fun totalDurationAtCapIsOk() {
        val result = AnimatedStickerValidator.validateTotalDuration(StickerPack.MAX_ANIMATION_DURATION_MS)
        assertEquals(AnimatedStickerValidator.Result.Ok, result)
    }

    @Test
    fun fileSizeAt500KbIsOk() {
        val result = AnimatedStickerValidator.validateFileSize(
            StickerPack.MAX_ANIMATED_STICKER_FILE_SIZE.toLong()
        )
        assertEquals(AnimatedStickerValidator.Result.Ok, result)
    }

    @Test
    fun fileSizeOverCapFails() {
        val result = AnimatedStickerValidator.validateFileSize(
            StickerPack.MAX_ANIMATED_STICKER_FILE_SIZE.toLong() + 1
        )
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.FileSizeTooLarge, result.reason)
    }

    @Test
    fun zeroFileSizeFails() {
        val result = AnimatedStickerValidator.validateFileSize(0)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
    }

    @Test
    fun nonSquareDimensionsFail() {
        val result = AnimatedStickerValidator.validateDimensions(512, 256)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.DimensionsInvalid, result.reason)
    }

    @Test
    fun perfect512SquareIsOk() {
        val result = AnimatedStickerValidator.validateDimensions(512, 512)
        assertEquals(AnimatedStickerValidator.Result.Ok, result)
    }

    @Test
    fun reasonableSpecValidates() {
        val spec = AnimatedStickerSpec(
            trimStartMs = 0L,
            trimEndMs = 3_000L,
            fps = 15,
            speed = 1f
        )
        assertEquals(AnimatedStickerValidator.Result.Ok, AnimatedStickerValidator.validateSpec(spec))
    }

    @Test
    fun specWithFpsOutOfRangeFails() {
        val spec = AnimatedStickerSpec(
            trimStartMs = 0L,
            trimEndMs = 3_000L,
            fps = AnimatedStickerSpec.MAX_FPS + 1,
            speed = 1f
        )
        val result = AnimatedStickerValidator.validateSpec(spec)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.FpsOutOfRange, result.reason)
    }

    @Test
    fun specWithReversedTrimRangeFails() {
        val spec = AnimatedStickerSpec(
            trimStartMs = 5_000L,
            trimEndMs = 1_000L,
            fps = 15,
            speed = 1f
        )
        val result = AnimatedStickerValidator.validateSpec(spec)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.TrimRangeInvalid, result.reason)
    }

    @Test
    fun specWithSpeedOutOfRangeFails() {
        val spec = AnimatedStickerSpec(
            trimStartMs = 0L,
            trimEndMs = 3_000L,
            fps = 15,
            speed = AnimatedStickerSpec.MAX_SPEED + 1f
        )
        val result = AnimatedStickerValidator.validateSpec(spec)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.SpeedOutOfRange, result.reason)
    }

    @Test
    fun specWithExcessiveDurationFails() {
        val spec = AnimatedStickerSpec(
            trimStartMs = 0L,
            trimEndMs = StickerPack.MAX_ANIMATION_DURATION_MS + 5_000L,
            fps = 15,
            speed = 1f
        )
        val result = AnimatedStickerValidator.validateSpec(spec)
        assertTrue(result is AnimatedStickerValidator.Result.Failure)
        assertEquals(AnimatedStickerValidator.Reason.TotalDurationTooLong, result.reason)
    }
}
