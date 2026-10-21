package data.util

internal actual fun releaseMemoryAfterStickerStep() = Unit

internal actual fun isOutOfMemoryError(throwable: Throwable): Boolean = false

internal actual fun deleteLocalFileQuietly(path: String) = Unit
