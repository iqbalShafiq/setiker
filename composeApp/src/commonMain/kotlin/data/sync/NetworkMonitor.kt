package data.sync

import kotlinx.coroutines.flow.StateFlow

expect class NetworkMonitor {
    val isOnline: StateFlow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
}
