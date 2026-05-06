package domain.actions

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import data.storage.StickerFileStorage
import domain.repository.StickerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidPackActions(
    private val context: Context,
    private val repository: StickerRepository,
    private val fileStorage: StickerFileStorage
) : PackActions {

    companion object {
        private const val TAG = "AndroidPackActions"
    }

    override suspend fun sharePack(packId: String) {
        withContext(Dispatchers.IO) {
            try {
                val pack = repository.getPack(packId)
                if (pack.stickers.isEmpty()) {
                    throw IllegalStateException("Pack has no stickers to share")
                }

                Log.d(TAG, "Sharing pack: ${pack.name} with ${pack.stickers.size} stickers")

                // Create cache directory for sharing
                val shareCacheDir = File(context.cacheDir, "share_stickers").apply { mkdirs() }
                
                // Clear old cache files
                shareCacheDir.listFiles()?.forEach { it.delete() }

                val stickerFiles = mutableListOf<File>()
                val missingFiles = mutableListOf<String>()

                pack.stickers.forEachIndexed { index, sticker ->
                    Log.d(TAG, "Checking sticker file: ${sticker.imageFile}")
                    val path = fileStorage.getImagePath(sticker.imageFile)
                    Log.d(TAG, "Full path: $path")
                    val file = File(path)
                    if (file.exists()) {
                        Log.d(TAG, "File exists: ${file.absolutePath}")
                        // Copy to cache directory with a clean name for sharing
                        val ext = file.extension.ifBlank { "webp" }
                        val cacheFile = File(shareCacheDir, "sticker_${index + 1}.$ext")
                        file.inputStream().use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        stickerFiles.add(cacheFile)
                    } else {
                        Log.w(TAG, "File NOT found: ${file.absolutePath}")
                        missingFiles.add(sticker.imageFile)
                    }
                }

                if (stickerFiles.isEmpty()) {
                    Log.e(TAG, "No sticker files found. Missing files: $missingFiles")
                    throw IllegalStateException("No sticker files found. Missing: ${missingFiles.joinToString(", ")}")
                }

                Log.d(TAG, "Found ${stickerFiles.size} sticker files to share")

                val uris = stickerFiles.map { file ->
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    putExtra(Intent.EXTRA_SUBJECT, pack.name)
                    putExtra(Intent.EXTRA_TEXT, "Check out my sticker pack: ${pack.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Share ${pack.name}")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share pack", e)
                throw IllegalStateException("Failed to share pack: ${e.message}")
            }
        }
    }

    override suspend fun addPackToWhatsApp(packId: String, packName: String): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val authority = "${context.packageName}.stickercontentprovider"
                val intent = Intent().apply {
                    action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
                    putExtra("sticker_pack_id", packId)
                    putExtra("sticker_pack_authority", authority)
                    putExtra("sticker_pack_name", packName)
                }

                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add pack to WhatsApp", e)
                false
            }
        }
    }

    override fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
}
