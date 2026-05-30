package data.util

/** Hint platform to release memory between heavy on-device sticker steps. */
internal expect fun releaseMemoryAfterStickerStep()

internal expect fun deleteLocalFileQuietly(path: String)
