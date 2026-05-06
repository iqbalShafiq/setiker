package com.setiker.app

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.room.Room
import data.local.database.StickerDatabase
import data.storage.StickerFileStorage
import kotlinx.coroutines.runBlocking
import java.io.File

class StickerContentProvider : ContentProvider() {

    companion object {
        const val METADATA = "metadata"
        const val STICKERS = "stickers"
        const val STICKERS_ASSET = "stickers_asset"

        const val METADATA_CODE = 1
        const val METADATA_CODE_FOR_SINGLE_PACK = 2
        const val STICKERS_CODE = 3
        const val STICKERS_ASSET_CODE = 4
        const val STICKER_PACK_TRAY_ICON_CODE = 5

        // Column names - MUST match WhatsApp's expected names exactly
        const val STICKER_PACK_IDENTIFIER = "sticker_pack_identifier"
        const val STICKER_PACK_NAME = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER = "sticker_pack_publisher"
        const val STICKER_PACK_ICON = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK = "ios_app_download_link"
        const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        const val AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"
        const val STICKER_FILE_NAME = "sticker_file_name"
        const val STICKER_FILE_EMOJI = "sticker_emoji"
        const val STICKER_FILE_ACCESSIBILITY_TEXT = "sticker_accessibility_text"

        private const val TAG = "StickerContentProvider"
    }

    private lateinit var uriMatcher: android.content.UriMatcher
    private var database: StickerDatabase? = null
    private var fileStorage: StickerFileStorage? = null

    private fun getDatabase(): StickerDatabase {
        if (database == null) {
            val context = context ?: throw IllegalStateException("Context is null")
            database = Room.databaseBuilder(
                context,
                StickerDatabase::class.java,
                StickerDatabase.DATABASE_NAME
            ).build()
        }
        return database!!
    }

    private fun getFileStorage(): StickerFileStorage {
        if (fileStorage == null) {
            val context = context ?: throw IllegalStateException("Context is null")
            fileStorage = StickerFileStorage(context)
        }
        return fileStorage!!
    }

    override fun onCreate(): Boolean {
        val authority = "${context?.packageName}.stickercontentprovider"
        uriMatcher = android.content.UriMatcher(android.content.UriMatcher.NO_MATCH).apply {
            addURI(authority, METADATA, METADATA_CODE)
            addURI(authority, "$METADATA/*", METADATA_CODE_FOR_SINGLE_PACK)
            addURI(authority, "$STICKERS/*", STICKERS_CODE)
            addURI(authority, "$STICKERS_ASSET/*/*", STICKERS_ASSET_CODE)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = runBlocking {
        Log.d(TAG, "query() called with uri: $uri")
        when (uriMatcher.match(uri)) {
            METADATA_CODE -> {
                Log.d(TAG, "Querying all packs metadata")
                getAllPacksCursor(uri)
            }
            METADATA_CODE_FOR_SINGLE_PACK -> {
                val identifier = uri.lastPathSegment ?: return@runBlocking null
                Log.d(TAG, "Querying single pack: $identifier")
                getPackCursor(uri, identifier)
            }
            STICKERS_CODE -> {
                val identifier = uri.pathSegments?.getOrNull(1) ?: return@runBlocking null
                Log.d(TAG, "Querying stickers for pack: $identifier")
                getStickersCursor(uri, identifier)
            }
            else -> {
                Log.e(TAG, "Unknown URI: $uri")
                throw IllegalArgumentException("Unknown URI: $uri")
            }
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? = runBlocking {
        Log.d(TAG, "openFile() called with uri: $uri")
        when (uriMatcher.match(uri)) {
            STICKERS_ASSET_CODE, STICKER_PACK_TRAY_ICON_CODE -> {
                val pathSegments = uri.pathSegments ?: return@runBlocking null
                if (pathSegments.size != 3) {
                    throw IllegalArgumentException("path segments should be 3, uri is: $uri")
                }
                val identifier = pathSegments[1]
                val fileName = pathSegments[2]
                
                if (identifier.isEmpty()) {
                    throw IllegalArgumentException("identifier is empty, uri: $uri")
                }
                if (fileName.isEmpty()) {
                    throw IllegalArgumentException("file name is empty, uri: $uri")
                }
                
                Log.d(TAG, "Opening file: $fileName for pack: $identifier")
                getStickerFile(fileName, identifier)
            }
            else -> {
                Log.w(TAG, "Unsupported URI for openFile: $uri")
                null
            }
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        Log.d(TAG, "openAssetFile() called with uri: $uri")
        val pfd = openFile(uri, mode) ?: return null
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            METADATA_CODE -> "vnd.android.cursor.dir/vnd.${context?.packageName}.stickercontentprovider.$METADATA"
            METADATA_CODE_FOR_SINGLE_PACK -> "vnd.android.cursor.item/vnd.${context?.packageName}.stickercontentprovider.$METADATA"
            STICKERS_CODE -> "vnd.android.cursor.dir/vnd.${context?.packageName}.stickercontentprovider.$STICKERS"
            STICKERS_ASSET_CODE -> "image/webp"
            STICKER_PACK_TRAY_ICON_CODE -> "image/png"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Not supported")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Not supported")
    }

    private suspend fun getAllPacksCursor(uri: Uri): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER, STICKER_PACK_NAME, STICKER_PACK_PUBLISHER,
            STICKER_PACK_ICON, ANDROID_APP_DOWNLOAD_LINK, IOS_APP_DOWNLOAD_LINK,
            PUBLISHER_EMAIL, PUBLISHER_WEBSITE, PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE, IMAGE_DATA_VERSION, AVOID_CACHE, ANIMATED_STICKER_PACK
        ))

        try {
            val packs = getDatabase().stickerPackDao().getAll()
            Log.d(TAG, "Found ${packs.size} packs in database")
            packs.forEach { entity ->
                val stickers = getDatabase().stickerDao().getByPackId(entity.identifier)
                val trayFileName = File(entity.trayImageFile ?: "").name
                Log.d(TAG, "Pack: ${entity.identifier}, trayImage: $trayFileName (original: ${entity.trayImageFile})")
                cursor.addRow(arrayOf(
                    entity.identifier,
                    entity.name,
                    entity.publisher,
                    trayFileName,
                    null, // android_play_store_link
                    null, // ios_app_download_link
                    null, // publisher_email
                    null, // publisher_website
                    null, // privacy_policy_website
                    null, // license_agreement_website
                    "1",  // image_data_version
                    0,    // avoid_cache
                    0     // animated_sticker_pack
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching packs from database", e)
        }

        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    private suspend fun getPackCursor(uri: Uri, identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER, STICKER_PACK_NAME, STICKER_PACK_PUBLISHER,
            STICKER_PACK_ICON, ANDROID_APP_DOWNLOAD_LINK, IOS_APP_DOWNLOAD_LINK,
            PUBLISHER_EMAIL, PUBLISHER_WEBSITE, PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE, IMAGE_DATA_VERSION, AVOID_CACHE, ANIMATED_STICKER_PACK
        ))

        try {
            val entity = getDatabase().stickerPackDao().getById(identifier)
            if (entity != null) {
                val trayFileName = File(entity.trayImageFile ?: "").name
                Log.d(TAG, "Pack: ${entity.identifier}, trayImage: $trayFileName (original: ${entity.trayImageFile})")
                cursor.addRow(arrayOf(
                    entity.identifier,
                    entity.name,
                    entity.publisher,
                    trayFileName,
                    null, null, null, null, null, null,
                    "1", 0, 0
                ))
            } else {
                Log.w(TAG, "Pack not found: $identifier")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pack: $identifier", e)
        }

        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    private suspend fun getStickersCursor(uri: Uri, identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(STICKER_FILE_NAME, STICKER_FILE_EMOJI, STICKER_FILE_ACCESSIBILITY_TEXT))

        try {
            val stickers = getDatabase().stickerDao().getByPackId(identifier)
            Log.d(TAG, "Found ${stickers.size} stickers for pack: $identifier")
            stickers.forEach { sticker ->
                val fileName = File(sticker.imageFile ?: "").name
                Log.d(TAG, "Sticker: $fileName (original: ${sticker.imageFile})")
                cursor.addRow(arrayOf(
                    fileName,
                    sticker.emojis ?: "",
                    sticker.accessibilityText ?: ""
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stickers for: $identifier", e)
        }

        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    private suspend fun getStickerFile(fileName: String, identifier: String): ParcelFileDescriptor? {
        return try {
            Log.d(TAG, "Getting sticker file: $fileName")
            val filePath = getFileStorage().getImagePath(fileName)
            Log.d(TAG, "Resolved path: $filePath")
            val file = File(filePath)
            if (file.exists()) {
                Log.d(TAG, "File exists, size: ${file.length()} bytes")
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                Log.e(TAG, "File not found: $filePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file: $fileName", e)
            null
        }
    }
}
