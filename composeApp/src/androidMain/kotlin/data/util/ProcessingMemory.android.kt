package data.util

import java.io.File

internal actual fun releaseMemoryAfterStickerStep() {
    System.gc()
    System.runFinalization()
}

internal actual fun deleteLocalFileQuietly(path: String) {
    runCatching { File(path).delete() }
}
