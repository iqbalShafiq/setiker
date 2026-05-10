package domain.util

import domain.model.AnimatedStickerSpec
import domain.model.StickerPack

/**
 * Pure validator for animated sticker constraints derived from WhatsApp's official spec
 * (see https://github.com/WhatsApp/stickers/blob/main/Android/README.md).
 */
object AnimatedStickerValidator {

    sealed class Result {
        data object Ok : Result()
        data class Failure(val reason: Reason) : Result()
    }

    enum class Reason {
        FrameDurationTooShort,
        TotalDurationTooLong,
        FileSizeTooLarge,
        DimensionsInvalid,
        FpsOutOfRange,
        SpeedOutOfRange,
        TrimRangeInvalid
    }

    fun validateFrameDuration(durationMs: Long): Result =
        if (durationMs >= StickerPack.MIN_FRAME_DURATION_MS) Result.Ok
        else Result.Failure(Reason.FrameDurationTooShort)

    fun validateTotalDuration(totalMs: Long): Result =
        if (totalMs in 1..StickerPack.MAX_ANIMATION_DURATION_MS) Result.Ok
        else Result.Failure(Reason.TotalDurationTooLong)

    fun validateFileSize(bytes: Long): Result =
        if (bytes in 1..StickerPack.MAX_ANIMATED_STICKER_FILE_SIZE.toLong()) Result.Ok
        else Result.Failure(Reason.FileSizeTooLarge)

    fun validateDimensions(width: Int, height: Int): Result =
        if (width == StickerPack.STICKER_SIZE && height == StickerPack.STICKER_SIZE) Result.Ok
        else Result.Failure(Reason.DimensionsInvalid)

    fun validateSpec(spec: AnimatedStickerSpec): Result {
        if (spec.fps !in AnimatedStickerSpec.MIN_FPS..AnimatedStickerSpec.MAX_FPS) {
            return Result.Failure(Reason.FpsOutOfRange)
        }
        if (spec.speed < AnimatedStickerSpec.MIN_SPEED || spec.speed > AnimatedStickerSpec.MAX_SPEED) {
            return Result.Failure(Reason.SpeedOutOfRange)
        }
        if (spec.trimEndMs <= spec.trimStartMs) {
            return Result.Failure(Reason.TrimRangeInvalid)
        }
        validateFrameDuration(spec.frameDurationMs).let { if (it is Result.Failure) return it }
        validateTotalDuration(spec.durationMs).let { if (it is Result.Failure) return it }
        return Result.Ok
    }
}
