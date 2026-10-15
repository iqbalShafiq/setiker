package permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationPermissionGate {
    private val _showRationale = MutableStateFlow(false)
    val showRationale: StateFlow<Boolean> = _showRationale.asStateFlow()

    /**
     * Shows the education sheet only when POST_NOTIFICATIONS is not already granted
     * (Android 13+). On older platforms, notifications are allowed by default so the
     * sheet is never shown.
     */
    fun requestWithRationale(context: Context) {
        if (hasNotificationPermission(context)) {
            _showRationale.value = false
            return
        }
        _showRationale.value = true
    }

    fun dismissRationale() {
        _showRationale.value = false
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
