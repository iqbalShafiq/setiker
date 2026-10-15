package permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.setiker.app.MainActivityHolder
import presentation.components.PermissionEducationBottomSheet
import presentation.theme.SetikerTheme

@Composable
fun NotificationPermissionHost() {
    val context = LocalContext.current
    val showRationale by NotificationPermissionGate.showRationale.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        NotificationPermissionGate.dismissRationale()
    }

    // If permission was granted elsewhere (settings / previous session), never keep the sheet open.
    LaunchedEffect(showRationale) {
        if (showRationale && NotificationPermissionGate.hasNotificationPermission(context)) {
            NotificationPermissionGate.dismissRationale()
        }
    }

    // Host sits outside App()'s theme in AndroidAppEntryPoint — wrap so sheet follows dark/light.
    SetikerTheme {
        PermissionEducationBottomSheet(
            visible = showRationale &&
                !NotificationPermissionGate.hasNotificationPermission(context),
            onDismiss = { NotificationPermissionGate.dismissRationale() },
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val activity = MainActivityHolder.current
                    if (activity != null &&
                        NotificationPermissionGate.hasNotificationPermission(activity)
                    ) {
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
}
