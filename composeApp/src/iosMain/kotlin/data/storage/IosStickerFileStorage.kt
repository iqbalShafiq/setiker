package data.storage

import domain.model.AnimatedStickerSpec
import domain.model.DecodedFrame
import domain.model.StickerDecoration
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
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

    actual suspend fun saveBytes(bytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val destPath = "$stickersDir/$fileName"
            bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }?.writeToFile(destPath, atomically = true)
            destPath
        }

    actual suspend fun readBytesAtPath(absolutePath: String): ByteArray? =
        withContext(Dispatchers.IO) {
            NSData.dataWithContentsOfFile(absolutePath)?.let { data ->
                ByteArray(data.length.toInt()).apply {
                    usePinned { pinned ->
                        memcpy(pinned.addressOf(0), data.bytes, data.length)
                    }
                }
            }
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

    actual suspend fun convertToWebP(sourcePath: String, outputFileName: String): String =
        withContext(Dispatchers.IO) {
            // iOS: For now, just copy the file. iOS WebP conversion would require platform-specific implementation
            saveImage(sourcePath, outputFileName)
        }

    actual suspend fun trySaveTrayImage(sourcePath: String, fileName: String): String? =
        withContext(Dispatchers.IO) {
            runCatching { saveImage(sourcePath, fileName) }.getOrNull()
        }

    actual suspend fun saveTrayImage(sourcePath: String, fileName: String): String =
        trySaveTrayImage(sourcePath, fileName)
            ?: throw IllegalStateException("Cannot save tray image: $sourcePath")

    actual suspend fun saveStickerImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            // iOS: For now, just copy the file
            saveImage(sourcePath, fileName)
        }

    actual suspend fun saveStickerImageWithDecorations(
        sourcePath: String,
        fileName: String,
        decorations: List<StickerDecoration>
    ): String = withContext(Dispatchers.IO) {
        if (decorations.isEmpty()) {
            return@withContext saveStickerImage(sourcePath, fileName)
        }
        val composedBytes = IosDecorationCompositor.compose(sourcePath, decorations)
        if (composedBytes != null) {
            saveBytes(composedBytes, fileName)
        } else {
            saveStickerImage(sourcePath, fileName)
        }
    }

    actual suspend fun getVideoDurationMs(videoPath: String): Long = -1L

    actual suspend fun extractVideoFrameToFile(
        videoPath: String,
        atMs: Long,
        fileName: String
    ): String? = null

    actual suspend fun decodeVideoFrames(
        videoPath: String,
        spec: AnimatedStickerSpec,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<DecodedFrame> {
        // iOS animated sticker pipeline (AVFoundation + libwebp) is not implemented yet.
        throw NotImplementedError("Animated sticker decoding is not implemented on iOS yet.")
    }

    actual suspend fun saveAnimatedStickerImage(
        frames: List<DecodedFrame>,
        fileName: String,
        baseDecorations: List<StickerDecoration>,
        frameDecorations: Map<Int, List<StickerDecoration>>,
        onProgress: (current: Int, total: Int) -> Unit
    ): String {
        throw NotImplementedError("Animated sticker encoding is not implemented on iOS yet.")
    }

    actual suspend fun encodeSingleFrameAnimatedWebP(
        sourcePath: String,
        fileName: String,
        decorations: List<StickerDecoration>
    ): String {
        // iOS doesn't have a libwebp animated encoder bundled yet; treat as static save so
        // builds stay green. WhatsApp pack validation only happens on Android right now.
        return saveStickerImage(sourcePath, fileName)
    }
}

@kotlinx.cinterop.ExperimentalForeignApi
private fun memcpy(dest: kotlinx.cinterop.CValuesRef<*>, src: kotlinx.cinterop.CValuesRef<*>, count: Int) {
    kotlinx.cinterop.memcpy(dest, src, count.toULong())
}