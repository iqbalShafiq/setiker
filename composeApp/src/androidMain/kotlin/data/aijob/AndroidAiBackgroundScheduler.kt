package data.aijob

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import permission.NotificationPermissionGate

actual class AiBackgroundScheduler(
    private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    actual fun scheduleJob(jobId: String, requiresNetwork: Boolean) {
        NotificationPermissionGate.requestWithRationale()
        val constraints = Constraints.Builder().apply {
            if (requiresNetwork) {
                setRequiredNetworkType(NetworkType.CONNECTED)
            }
        }.build()
        val request = OneTimeWorkRequestBuilder<AiJobProcessorWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            AiNotificationHelper.PROCESSOR_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    actual fun cancelJob(jobId: String) {
        // Cooperative cancellation is handled in Room; processor will observe status.
    }

    actual fun resumeQueuedJobs() {
        val request = OneTimeWorkRequestBuilder<AiJobProcessorWorker>().build()
        workManager.enqueueUniqueWork(
            AiNotificationHelper.PROCESSOR_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
