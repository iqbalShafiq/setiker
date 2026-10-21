package domain.util

import domain.model.AnimatedStickerSpec
import domain.model.StickerPack

/**
 * Pure validator for animated sticker constraints derived from WhatsApp's official spec
 * (see https://github.com/WhatsApp/stickers/blob/main/Android/README.md).
 *
 * [StickerValidation] used to be named `Result`, which collided with `kotlin.Result` at every
 * call site that also needed the typed `domain.error.Result` from P3b.
 */
object AnimatedStickerValidator {

    sealed class StickerValidation {
        data object Ok : StickerValidation()
        data class Failure(val reason: Reason) : StickerValidation()
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

    fun validateFrameDuration(durationMs: Long): StickerValidation =
        if (durationMs >= StickerPack.MIN_FRAME_DURATION_MS) StickerValidation.Ok
        else StickerValidation.Failure(Reason.FrameDurationTooShort)

    fun validateTotalDuration(totalMs: Long): StickerValidation =
        if (totalMs in 1..StickerPack.MAX_ANIMATION_DURATION_MS) StickerValidation.Ok
        else StickerValidation.Failure(Reason.TotalDurationTooLong)

    fun validateFileSize(bytes: Long): StickerValidation =
        if (bytes in 1..StickerPack.MAX_ANIMATED_STICKER_FILE_SIZE.toLong()) StickerValidation.Ok
        else StickerValidation.Failure(Reason.FileSizeTooLarge)

    fun validateDimensions(width: Int, height: Int): StickerValidation =
        if (width == StickerPack.STICKER_SIZE && height == StickerPack.STICKER_SIZE) StickerValidation.Ok
        else StickerValidation.Failure(Reason.DimensionsInvalid)

    fun validateSpec(spec: AnimatedStickerSpec): StickerValidation {
        if (spec.fps !in AnimatedStickerSpec.MIN_FPS..AnimatedStickerSpec.MAX_FPS) {
            return StickerValidation.Failure(Reason.FpsOutOfRange)
        }
        if (spec.speed < AnimatedStickerSpec.MIN_SPEED || spec.speed > AnimatedStickerSpec.MAX_SPEED) {
            return StickerValidation.Failure(Reason.SpeedOutOfRange)
        }
        if (spec.trimEndMs <= spec.trimStartMs) {
            return StickerValidation.Failure(Reason.TrimRangeInvalid)
        }
        validateFrameDuration(spec.frameDurationMs).let { if (it is StickerValidation.Failure) return it }
        validateTotalDuration(spec.durationMs).let { if (it is StickerValidation.Failure) return it }
        return StickerValidation.Ok
    }
}
