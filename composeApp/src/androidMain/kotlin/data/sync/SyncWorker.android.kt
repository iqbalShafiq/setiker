package data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import domain.model.SyncResult
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.TimeUnit

actual class SyncWorker(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    
    actual fun scheduleImmediateSync() {
        val workRequest = OneTimeWorkRequestBuilder<SyncWorkerTask>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork("immediate_sync", ExistingWorkPolicy.REPLACE, workRequest)
    }
    
    actual fun schedulePeriodicSync() {
        val workRequest = PeriodicWorkRequestBuilder<SyncWorkerTask>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork("periodic_sync", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }
    
    actual fun cancelAllSyncWork() {
        workManager.cancelUniqueWork("immediate_sync")
        workManager.cancelUniqueWork("periodic_sync")
    }
}

class SyncWorkerTask(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val syncManager: SyncManager by inject(SyncManager::class.java)
    
    override suspend fun doWork(): Result {
        return try {
            val report = syncManager.sync()
            when (report.result) {
                is SyncResult.Failed -> Result.retry()
                SyncResult.Success,
                SyncResult.SkippedNotAuthenticated,
                SyncResult.SkippedInProgress,
                SyncResult.SkippedOffline -> Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
