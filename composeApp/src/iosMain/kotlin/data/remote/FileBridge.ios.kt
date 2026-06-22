package data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
actual fun readFileBytes(path: String): ByteArray {
    val nsData = NSData.dataWithContentsOfFile(path) ?: return ByteArray(0)
    return ByteArray(nsData.length.toInt()).apply {
        usePinned { pinned ->
            kotlinx.cinterop.memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
        }
    }
}
