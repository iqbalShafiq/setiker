package util

import androidx.compose.ui.graphics.ImageBitmap

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    // Animated sticker editor is not wired on iOS yet.
    return null
}

actual suspend fun decodeImageBitmapInfo(filePath: String): ImageBitmapInfo? = null
