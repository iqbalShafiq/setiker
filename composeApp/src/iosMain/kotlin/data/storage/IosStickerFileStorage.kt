package data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

actual class StickerFileStorage {

    private val stickersDir: String
        get() {
            val paths = NSFileManager.defaultManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            )
            val documentsDir = paths.firstOrNull()?.path ?: ""
            val stickersPath = "$documentsDir/stickers"
            NSFileManager.defaultManager.createDirectoryAtPath(
                stickersPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            return stickersPath
        }

    actual suspend fun saveImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val destPath = "$stickersDir/$fileName"
            val sourceData = NSData.dataWithContentsOfFile(sourcePath)
            sourceData?.writeToFile(destPath, atomically = true)
            destPath
        }

    actual suspend fun loadImage(fileName: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val filePath = "$stickersDir/$fileName"
            NSData.dataWithContentsOfFile(filePath)?.let { data ->
                ByteArray(data.length.toInt()).apply {
                    usePinned { pinned ->
                        memcpy(pinned.addressOf(0), data.bytes, data.length)
                    }
                }
            }
        }

    actual suspend fun deleteImage(fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            val filePath = "$stickersDir/$fileName"
            NSFileManager.defaultManager.removeItemAtPath(filePath, null)
            true
        }

    actual suspend fun getImagePath(fileName: String): String =
        "$stickersDir/$fileName"

    actual suspend fun imageExists(fileName: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath("$stickersDir/$fileName")
}

@kotlinx.cinterop.ExperimentalForeignApi
private fun memcpy(dest: kotlinx.cinterop.CValuesRef<*>, src: kotlinx.cinterop.CValuesRef<*>, count: Int) {
    kotlinx.cinterop.memcpy(dest, src, count.toULong())
}