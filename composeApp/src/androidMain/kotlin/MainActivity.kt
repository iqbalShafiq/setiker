package com.setiker.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    
    private val whatsAppLauncher = WhatsAppStickerLauncher()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        whatsAppLauncher.register(this) { success, error ->
            if (!success) {
                // Handle error - could show a toast or snackbar
                // For now, we'll just log it
                android.util.Log.e("MainActivity", "WhatsApp error: $error")
            }
        }
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        setContent {
            App(
                onAddToWhatsApp = { packId, packName ->
                    if (WhatsAppStickerLauncher.isWhatsAppInstalled(packageManager)) {
                        whatsAppLauncher.launchAddToWhatsApp(this, packId, packName)
                    }
                }
            )
        }
    }
}
