package data.remote

import com.setiker.app.SetikerRuntimeConfig

actual object ApiConfig {
    actual val baseUrl: String = "http://13.251.98.162:8080"
    actual val isDebugLoggingEnabled: Boolean get() = SetikerRuntimeConfig.isDebuggable
}
