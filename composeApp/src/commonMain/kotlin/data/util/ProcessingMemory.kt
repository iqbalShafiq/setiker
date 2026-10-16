package data.util

/** Hint platform to release memory between heavy on-device sticker steps. */
internal expect fun releaseMemoryAfterStickerStep()

/**
 * True when [throwable] represents an out-of-memory failure on this platform.
 *
 * Android surfaces allocation failures as [OutOfMemoryError]; Kotlin/Native aborts the process
 * instead of throwing, so the Apple actual always answers `false`.
 */
internal expect fun isOutOfMemoryError(throwable: Throwable): Boolean

internal expect fun deleteLocalFileQuietly(path: String)
