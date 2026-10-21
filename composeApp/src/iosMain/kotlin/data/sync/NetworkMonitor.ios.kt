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

    /**
     * How many callers want the monitor running.
     *
     * The path monitor is a system resource. Starting it twice would drop the first one on the
     * floor without cancelling it, so the count keeps exactly one monitor alive and cancels it
     * when the last caller is gone.
     */
    private var registrations = 0

    actual fun startMonitoring() {
        registrations++
        if (registrations > 1) return
        if (monitor != null) return

        val pathMonitor = nw_path_monitor_create()
        monitor = pathMonitor
        nw_path_monitor_set_queue(pathMonitor, monitorQueue)

        nw_path_monitor_set_update_handler(pathMonitor) { path ->
            val isOnline = nw_path_get_status(path) == nw_path_status_satisfied
            _isOnline.value = isOnline
        }

        nw_path_monitor_start(pathMonitor)
    }

    actual fun stopMonitoring() {
        if (registrations == 0) return
        registrations--
        if (registrations > 0) return

        monitor?.let {
            nw_path_monitor_cancel(it)
            monitor = null
        }
    }
}
