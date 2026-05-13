package data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Network.*
import platform.darwin.dispatch_queue_create

actual class NetworkMonitor {
    private val _isOnline = MutableStateFlow(false)
    actual val isOnline: StateFlow<Boolean> = _isOnline

    private var monitor: nw_path_monitor_t? = null
    private val monitorQueue = dispatch_queue_create("NetworkMonitor", null)

    actual fun startMonitoring() {
        monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor!!, monitorQueue)

        nw_path_monitor_set_update_handler(monitor!!) { path ->
            val isOnline = nw_path_get_status(path) == nw_path_status_satisfied
            _isOnline.value = isOnline
        }

        nw_path_monitor_start(monitor!!)
    }

    actual fun stopMonitoring() {
        monitor?.let {
            nw_path_monitor_cancel(it)
            monitor = null
        }
    }
}
