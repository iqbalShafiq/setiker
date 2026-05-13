package data.sync

actual class SyncWorker {
    actual fun scheduleImmediateSync() {
        // iOS: Trigger sync when app comes to foreground
    }
    
    actual fun schedulePeriodicSync() {
        // iOS: Use BGTaskScheduler for background refresh
    }
    
    actual fun cancelAllSyncWork() {
        // iOS: Cancel background tasks
    }
}
