package com.setiker.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import data.storage.StickerFileStorage
import domain.repository.StickerRepository
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.io.File

class StickerContentProvider : ContentProvider() {

    companion object {
        const val METADATA_CODE = 1
        const val METADATA_CODE_FOR_SINGLE_PACK = 2
        const val STICKERS_CODE = 3
        const val STICKERS_ASSET_CODE = 4

        const val STICKER_PACK_IDENTIFIER = "sticker_pack_identifier"
        const val STICKER_PACK_NAME = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER = "sticker_pack_publisher"
        const val STICKER_PACK_ICON = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK = "android_app_download_link"
        const val IOS_APP_DOWNLOAD_LINK = "ios_app_download_link"
        const val PUBLISHER_EMAIL = "publisher_email"
        const val PUBLISHER_WEBSITE = "publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE = "license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        const val AVOID_CACHE = "avoid_cache"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"
        const val STICKER_FILE_NAME = "sticker_file_name"
        const val STICKER_FILE_EMOJI = "sticker_file_emoji"
    }

    private val repository: StickerRepository by inject()
    private val fileStorage: StickerFileStorage by inject()

    private lateinit var uriMatcher: android.content.UriMatcher

    override fun onCreate(): Boolean {
        val authority = "${context?.packageName}.stickercontentprovider"
        uriMatcher = android.content.UriMatcher(android.content.UriMatcher.NO_MATCH).apply {
            addURI(authority, "metadata", METADATA_CODE)
            addURI(authority, "metadata/*", METADATA_CODE_FOR_SINGLE_PACK)
            addURI(authority, "stickers/*", STICKERS_CODE)
            addURI(authority, "stickers_asset/*/*", STICKERS_ASSET_CODE)
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
        when (uriMatcher.match(uri)) {
            METADATA_CODE -> getAllPacksCursor()
            METADATA_CODE_FOR_SINGLE_PACK -> {
                val identifier = uri.lastPathSegment ?: return@runBlocking null
                getPackCursor(identifier)
            }
            STICKERS_CODE -> {
                val identifier = uri.pathSegments?.getOrNull(1) ?: return@runBlocking null
                getStickersCursor(identifier)
            }
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? = runBlocking {
        when (uriMatcher.match(uri)) {
            STICKERS_ASSET_CODE -> {
                val pathSegments = uri.pathSegments ?: return@runBlocking null
                val fileName = pathSegments.getOrNull(2) ?: return@runBlocking null
                val filePath = fileStorage.getImagePath(fileName)
                val file = File(filePath)
                if (file.exists()) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    private suspend fun getAllPacksCursor(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER, STICKER_PACK_NAME, STICKER_PACK_PUBLISHER,
            STICKER_PACK_ICON, ANDROID_APP_DOWNLOAD_LINK, IOS_APP_DOWNLOAD_LINK,
            PUBLISHER_EMAIL, PUBLISHER_WEBSITE, PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE, IMAGE_DATA_VERSION, AVOID_CACHE, ANIMATED_STICKER_PACK
        ))

        try {
            val packs = repository.getAllPacks()
            packs.forEach { pack ->
                cursor.addRow(arrayOf(
                    pack.identifier,
                    pack.name,
                    pack.publisher,
                    pack.trayImageFile,
                    null, // android_app_download_link
                    null, // ios_app_download_link
                    null, // publisher_email
                    null, // publisher_website
                    null, // privacy_policy_website
                    null, // license_agreement_website
                    "1",  // image_data_version
                    "0",  // avoid_cache
                    "0"   // animated_sticker_pack
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cursor
    }

    private suspend fun getPackCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER, STICKER_PACK_NAME, STICKER_PACK_PUBLISHER,
            STICKER_PACK_ICON, ANDROID_APP_DOWNLOAD_LINK, IOS_APP_DOWNLOAD_LINK,
            PUBLISHER_EMAIL, PUBLISHER_WEBSITE, PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE, IMAGE_DATA_VERSION, AVOID_CACHE, ANIMATED_STICKER_PACK
        ))

        try {
            val pack = repository.getPack(identifier)
            cursor.addRow(arrayOf(
                pack.identifier,
                pack.name,
                pack.publisher,
                pack.trayImageFile,
                null, null, null, null, null, null,
                "1", "0", "0"
            ))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cursor
    }

    private suspend fun getStickersCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(STICKER_FILE_NAME, STICKER_FILE_EMOJI))

        try {
            val pack = repository.getPack(identifier)
            pack.stickers.forEach { sticker ->
                cursor.addRow(arrayOf(
                    sticker.imageFile,
                    sticker.emojis.joinToString(",")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cursor
    }
}