package util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decode a PNG/JPEG byte array into a Compose [ImageBitmap]. Returns null on failure.
 *
 * Used by the animated sticker editor to render decoded video frames in the preview area
 * without going through Coil/disk I/O.
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

data class ImageBitmapInfo(val width: Int, val height: Int)

/**
 * Cheap dimensions probe that does NOT decode pixel data into memory. Used by
 * `VideoCropViewModel` to know the source frame's aspect ratio before showing the cropper.
 */
expect suspend fun decodeImageBitmapInfo(filePath: String): ImageBitmapInfo?
