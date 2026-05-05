package com.setiker.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import domain.model.StickerPack
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext

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

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        // Implement query logic for metadata
        return when (uriMatcher.match(uri)) {
            METADATA_CODE -> getAllPacksCursor()
            METADATA_CODE_FOR_SINGLE_PACK -> {
                val identifier = uri.lastPathSegment ?: return null
                getPackCursor(identifier)
            }
            STICKERS_CODE -> {
                val identifier = uri.pathSegments[1] ?: return null
                getStickersCursor(identifier)
            }
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return when (uriMatcher.match(uri)) {
            STICKERS_ASSET_CODE -> {
                // Return sticker file
                null
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    private fun getAllPacksCursor(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER,
            STICKER_PACK_NAME,
            STICKER_PACK_PUBLISHER,
            STICKER_PACK_ICON,
            ANDROID_APP_DOWNLOAD_LINK,
            IOS_APP_DOWNLOAD_LINK,
            PUBLISHER_EMAIL,
            PUBLISHER_WEBSITE,
            PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE,
            IMAGE_DATA_VERSION,
            AVOID_CACHE,
            ANIMATED_STICKER_PACK
        ))
        
        // TODO: Get all packs from repository and add rows
        
        return cursor
    }

    private fun getPackCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER,
            STICKER_PACK_NAME,
            STICKER_PACK_PUBLISHER,
            STICKER_PACK_ICON,
            ANDROID_APP_DOWNLOAD_LINK,
            IOS_APP_DOWNLOAD_LINK,
            PUBLISHER_EMAIL,
            PUBLISHER_WEBSITE,
            PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE,
            IMAGE_DATA_VERSION,
            AVOID_CACHE,
            ANIMATED_STICKER_PACK
        ))
        
        // TODO: Get pack from repository and add row
        
        return cursor
    }

    private fun getStickersCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_FILE_NAME,
            STICKER_FILE_EMOJI
        ))
        
        // TODO: Get stickers from pack and add rows
        
        return cursor
    }

    private val uriMatcher = android.content.UriMatcher(android.content.UriMatcher.NO_MATCH).apply {
        addURI("${context?.packageName}.stickercontentprovider", "metadata", METADATA_CODE)
        addURI("${context?.packageName}.stickercontentprovider", "metadata/*", METADATA_CODE_FOR_SINGLE_PACK)
        addURI("${context?.packageName}.stickercontentprovider", "stickers/*", STICKERS_CODE)
        addURI("${context?.packageName}.stickercontentprovider", "stickers_asset/*/*", STICKERS_ASSET_CODE)
    }
}
