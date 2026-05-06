package com.setiker.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class WhatsAppStickerLauncher {
    
    companion object {
        private const val TAG = "WhatsAppStickerLauncher"
        private const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        private const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
        private const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
        private const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"
        
        const val CONSUMER_WHATSAPP_PACKAGE_NAME = "com.whatsapp"
        const val SMB_WHATSAPP_PACKAGE_NAME = "com.whatsapp.w4b"
        private const val STICKER_APP_AUTHORITY_SUFFIX = ".stickercontentprovider"
        
        fun isWhatsAppInstalled(packageManager: PackageManager): Boolean {
            return isPackageInstalled(CONSUMER_WHATSAPP_PACKAGE_NAME, packageManager) ||
                    isPackageInstalled(SMB_WHATSAPP_PACKAGE_NAME, packageManager)
        }
        
        private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
            return try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
        
        fun isWhatsAppConsumerInstalled(packageManager: PackageManager): Boolean {
            return isPackageInstalled(CONSUMER_WHATSAPP_PACKAGE_NAME, packageManager)
        }
        
        fun isWhatsAppSmbInstalled(packageManager: PackageManager): Boolean {
            return isPackageInstalled(SMB_WHATSAPP_PACKAGE_NAME, packageManager)
        }
    }
    
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var onResultCallback: ((Boolean, String?) -> Unit)? = null
    
    fun register(activity: ComponentActivity, onResult: (Boolean, String?) -> Unit) {
        onResultCallback = onResult
        launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    onResultCallback?.invoke(true, null)
                }
                Activity.RESULT_CANCELED -> {
                    val error = result.data?.getStringExtra("validation_error")
                    onResultCallback?.invoke(false, error ?: "Sticker pack was not added")
                }
                else -> {
                    onResultCallback?.invoke(false, "Unknown error occurred")
                }
            }
        }
    }
    
    fun launchAddToWhatsApp(activity: Activity, packId: String, packName: String) {
        try {
            val authority = "${activity.packageName}$STICKER_APP_AUTHORITY_SUFFIX"
            val intent = Intent().apply {
                action = ACTION_ENABLE_STICKER_PACK
                putExtra(EXTRA_STICKER_PACK_ID, packId)
                putExtra(EXTRA_STICKER_PACK_AUTHORITY, authority)
                putExtra(EXTRA_STICKER_PACK_NAME, packName)
            }
            
            val consumerInstalled = isWhatsAppConsumerInstalled(activity.packageManager)
            val smbInstalled = isWhatsAppSmbInstalled(activity.packageManager)
            
            when {
                !consumerInstalled && !smbInstalled -> {
                    onResultCallback?.invoke(false, "WhatsApp is not installed")
                }
                consumerInstalled && smbInstalled -> {
                    // Both installed, show chooser
                    val chooserIntent = Intent.createChooser(intent, "Add to WhatsApp")
                    launcher?.launch(chooserIntent) ?: activity.startActivity(chooserIntent)
                }
                consumerInstalled -> {
                    intent.setPackage(CONSUMER_WHATSAPP_PACKAGE_NAME)
                    launcher?.launch(intent) ?: activity.startActivity(intent)
                }
                smbInstalled -> {
                    intent.setPackage(SMB_WHATSAPP_PACKAGE_NAME)
                    launcher?.launch(intent) ?: activity.startActivity(intent)
                }
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Activity not found", e)
            onResultCallback?.invoke(false, "WhatsApp is not installed or not supported")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching WhatsApp", e)
            onResultCallback?.invoke(false, e.message ?: "Failed to add pack to WhatsApp")
        }
    }
}
