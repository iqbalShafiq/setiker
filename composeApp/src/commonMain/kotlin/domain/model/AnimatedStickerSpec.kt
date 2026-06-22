package domain.model

import kotlinx.serialization.Serializable

/**
 * Spec that drives video → animated sticker decoding.
 *
 * WhatsApp constraints:
 * - Total animation ≤ 10_000 ms (clamped via [trimEndMs] - [trimStartMs]).
 * - Frame duration ≥ 8 ms (so fps ≤ 125; we cap to MAX_FPS for sane defaults).
 * - Output sticker exactly 512×512.
 */
/**
 * User-controlled crop transform applied to every video frame at decode time, so we never
 * stretch source pixels — instead we letterbox/cover/zoom according to user intent and end
 * up at WhatsApp's required 512×512 output.
 *
 * All offsets are normalized to the 512×512 output box ([-1, 1]). [scale] multiplies on top
 * of an aspect-fit scale computed from each source frame's dimensions, so the preview the
 * user sees in `VideoCropScreen` matches the final saved frame regardless of source size.
 *
 * When `null`, the legacy aspect-fill center-crop is applied (kept for back-compat /
 * iOS stub paths).
 */
@Serializable
data class CropTransform(
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val offsetXNorm: Float = 0f,
    val offsetYNorm: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
) {
    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 4f
    }
}

@Serializable
data class AnimatedStickerSpec(
    val trimStartMs: Long,
    val trimEndMs: Long,
    val fps: Int = DEFAULT_FPS,
    val speed: Float = 1f,
    val loopCount: Int = 0,
    val cropTransform: CropTransform? = null
) {
    val durationMs: Long
        get() = ((trimEndMs - trimStartMs).coerceAtLeast(0L) / speed.coerceAtLeast(MIN_SPEED)).toLong()

    val frameDurationMs: Long
        get() = (1000L / fps.coerceIn(MIN_FPS, MAX_FPS)).coerceAtLeast(StickerPack.MIN_FRAME_DURATION_MS)

    val frameCount: Int
        get() = (durationMs / frameDurationMs).toInt().coerceAtLeast(1)

    companion object {
        const val MIN_FPS = 8
        const val MAX_FPS = 30
        const val DEFAULT_FPS = 15
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 4f
    }
}

/**
 * A decoded frame ready for re-encoding to animated WebP.
 *
 * [bytes] is a PNG-encoded 512×512 bitmap so we can pass it across the KMP boundary
 * without leaking platform `Bitmap` types into commonMain.
 */
data class DecodedFrame(
    val bytes: ByteArray,
    val durationMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedFrame) return false
        if (durationMs != other.durationMs) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}
