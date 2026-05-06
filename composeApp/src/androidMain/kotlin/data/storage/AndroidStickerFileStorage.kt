package data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

actual class StickerFileStorage(private val context: Context) {

    private val stickersDir: File
        get() = File(context.filesDir, "stickers").apply { mkdirs() }

    actual suspend fun saveImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val destFile = File(stickersDir, fileName)
            File(sourcePath).inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        }

    actual suspend fun loadImage(fileName: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = File(stickersDir, fileName)
            if (file.exists()) file.readBytes() else null
        }

    actual suspend fun deleteImage(fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            File(stickersDir, fileName).delete()
        }

    actual suspend fun getImagePath(fileName: String): String {
        // Always resolve against stickers directory using basename
        val basename = File(fileName).name
        return File(stickersDir, basename).absolutePath
    }

    actual suspend fun imageExists(fileName: String): Boolean =
        File(stickersDir, fileName).exists()

    actual suspend fun convertToWebP(sourcePath: String, outputFileName: String): String =
        withContext(Dispatchers.IO) {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                throw IllegalArgumentException("Source file does not exist: $sourcePath")
            }

            // If already WebP, just copy to stickers directory
            if (sourceFile.extension.equals("webp", ignoreCase = true)) {
                return@withContext saveImage(sourcePath, outputFileName)
            }

            // Convert to WebP 512x512
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                // Resize to 512x512 while maintaining aspect ratio
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

                val destFile = File(stickersDir, outputFileName)
                FileOutputStream(destFile).use { out ->
                    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    scaledBitmap.compress(format, 90, out)
                }

                // Cleanup
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    /**
     * Save tray icon for WhatsApp.
     * Requirements: 96x96px, max 50KB, PNG format
     */
    actual suspend fun saveTrayImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                // Resize to 96x96
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, true)

                // Compress as PNG, reduce quality if needed to stay under 50KB
                var quality = 100
                var bytes: ByteArray
                do {
                    val stream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
                    bytes = stream.toByteArray()
                    quality -= 10
                } while (bytes.size > 50 * 1024 && quality > 10)

                val destFile = File(stickersDir, fileName)
                destFile.writeBytes(bytes)

                android.util.Log.d("StickerFileStorage", 
                    "Tray icon saved: ${destFile.absolutePath}, size: ${bytes.size} bytes (${bytes.size / 1024}KB)")

                // Cleanup
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }

    /**
     * Save sticker image for WhatsApp.
     * Requirements: 512x512px, max 100KB, WebP format
     */
    actual suspend fun saveStickerImage(sourcePath: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: throw IllegalArgumentException("Cannot decode image: $sourcePath")

            try {
                // Resize to 512x512
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

                // Compress as WebP, reduce quality if needed to stay under 100KB
                var quality = 90
                var bytes: ByteArray
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }

                do {
                    val stream = ByteArrayOutputStream()
                    scaledBitmap.compress(format, quality, stream)
                    bytes = stream.toByteArray()
                    quality -= 5
                } while (bytes.size > 100 * 1024 && quality > 20)

                val destFile = File(stickersDir, fileName)
                destFile.writeBytes(bytes)

                android.util.Log.d("StickerFileStorage", 
                    "Sticker saved: ${destFile.absolutePath}, size: ${bytes.size} bytes (${bytes.size / 1024}KB)")

                // Cleanup
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                destFile.absolutePath
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        }
}
