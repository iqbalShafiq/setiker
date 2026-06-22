package presentation.common

import domain.util.AnimatedStickerValidator
import org.jetbrains.compose.resources.StringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.animated_error_decode_failed
import setiker.composeapp.generated.resources.animated_error_draft_not_found
import setiker.composeapp.generated.resources.animated_error_encode_size_limit
import setiker.composeapp.generated.resources.animated_error_file_too_large
import setiker.composeapp.generated.resources.animated_error_fps_out_of_range
import setiker.composeapp.generated.resources.animated_error_frame_duration_too_short
import setiker.composeapp.generated.resources.animated_error_frames_missing
import setiker.composeapp.generated.resources.animated_error_invalid_dimensions
import setiker.composeapp.generated.resources.animated_error_missing_files
import setiker.composeapp.generated.resources.animated_error_no_frames
import setiker.composeapp.generated.resources.animated_error_save_failed
import setiker.composeapp.generated.resources.animated_error_speed_out_of_range
import setiker.composeapp.generated.resources.animated_error_total_duration_too_long
import setiker.composeapp.generated.resources.animated_error_too_many_frames
import setiker.composeapp.generated.resources.animated_error_trim_invalid

fun AnimatedStickerValidator.Reason.toUiText(): UiText =
    UiText.StringRes(toStringResource())

private fun AnimatedStickerValidator.Reason.toStringResource(): StringResource = when (this) {
    AnimatedStickerValidator.Reason.FrameDurationTooShort -> Res.string.animated_error_frame_duration_too_short
    AnimatedStickerValidator.Reason.TotalDurationTooLong -> Res.string.animated_error_total_duration_too_long
    AnimatedStickerValidator.Reason.FileSizeTooLarge -> Res.string.animated_error_file_too_large
    AnimatedStickerValidator.Reason.DimensionsInvalid -> Res.string.animated_error_invalid_dimensions
    AnimatedStickerValidator.Reason.FpsOutOfRange -> Res.string.animated_error_fps_out_of_range
    AnimatedStickerValidator.Reason.SpeedOutOfRange -> Res.string.animated_error_speed_out_of_range
    AnimatedStickerValidator.Reason.TrimRangeInvalid -> Res.string.animated_error_trim_invalid
}

/**
 * Maps known animated-sticker failure text (from jobs, storage, or legacy copy) to localized [UiText].
 * Returns null when the message is not recognized so callers can fall back to [defaultRes].
 */
fun animatedFailureMessageToUiText(message: String?): UiText? {
    if (message.isNullOrBlank()) return null
    val normalized = message.lowercase()
    val resource = when {
        normalized.contains("draft not found") -> Res.string.animated_error_draft_not_found
        normalized.contains("frame files are missing") ||
            normalized.contains("animated frame files are missing") ||
            normalized.contains("animated frame file not found") -> Res.string.animated_error_frames_missing
        normalized.contains("no frames to encode") -> Res.string.animated_error_no_frames
        normalized.contains("cannot fit animated sticker") ||
            normalized.contains("under") && normalized.contains("kb") -> Res.string.animated_error_encode_size_limit
        normalized.contains("cannot decode frame") ||
            normalized.contains("cannot decode image") ||
            normalized.contains("decode") && normalized.contains("frame") -> Res.string.animated_error_decode_failed
        normalized.contains("missing") && normalized.contains("file") -> Res.string.animated_error_missing_files
        normalized.contains("too many frame") -> Res.string.animated_error_too_many_frames
        normalized.contains("too large") || normalized.contains("file size") -> Res.string.animated_error_file_too_large
        normalized.contains("dimension") || normalized.contains("512") -> Res.string.animated_error_invalid_dimensions
        normalized.contains("failed to save animated") -> Res.string.animated_error_save_failed
        else -> null
    }
    return resource?.let { UiText.StringRes(it) }
}

fun Throwable.toAnimatedUiText(defaultRes: StringResource = Res.string.animated_error_save_failed): UiText {
    animatedFailureMessageToUiText(message)?.let { return it }
    return UiText.StringRes(defaultRes)
}
