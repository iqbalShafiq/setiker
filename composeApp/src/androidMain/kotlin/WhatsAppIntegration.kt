package com.setiker.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import domain.model.StickerPack

object WhatsAppIntegration {
    
    const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"
    
    fun addPackToWhatsApp(
        activity: Activity,
        pack: StickerPack,
        authority: String
    ) {
        val intent = Intent().apply {
            action = ACTION_ENABLE_STICKER_PACK
            putExtra(EXTRA_STICKER_PACK_ID, pack.identifier)
            putExtra(EXTRA_STICKER_PACK_AUTHORITY, authority)
            putExtra(EXTRA_STICKER_PACK_NAME, pack.name)
        }
        
        try {
            activity.startActivityForResult(intent, 200)
        } catch (e: Exception) {
            throw IllegalStateException("WhatsApp is not installed or unable to handle sticker packs")
        }
    }
    
    fun isWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
