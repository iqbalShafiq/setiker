package permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.setiker.app.MainActivityHolder
import presentation.components.PermissionEducationBottomSheet

@Composable
fun NotificationPermissionHost() {
    val showRationale by NotificationPermissionGate.showRationale.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        NotificationPermissionGate.dismissRationale()
    }

    PermissionEducationBottomSheet(
        visible = showRationale,
        onDismiss = { NotificationPermissionGate.dismissRationale() },
        onAllow = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val activity = MainActivityHolder.current
                val granted = activity?.let {
                    ContextCompat.checkSelfPermission(it, Manifest.permission.POST_NOTIFICATIONS)
                }
                if (granted == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    NotificationPermissionGate.dismissRationale()
                } else {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                NotificationPermissionGate.dismissRationale()
            }
        }
    )
}
