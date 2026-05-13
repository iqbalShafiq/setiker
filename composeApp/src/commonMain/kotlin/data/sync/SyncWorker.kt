package data.sync

expect class SyncWorker {
    fun scheduleImmediateSync()
    fun schedulePeriodicSync()
    fun cancelAllSyncWork()
}
