package data.aijob

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import domain.model.aijob.AiJobStatus
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.java.KoinJavaComponent.inject

class AiJobProcessorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val jobRepository: AiJobRepository by inject(AiJobRepository::class.java)
    private val draftRepository: WorkspaceDraftRepository by inject(WorkspaceDraftRepository::class.java)
    private val aiJobManager: AiJobManager by inject(AiJobManager::class.java)
    private val runner: AiJobRunner by inject(AiJobRunner::class.java)
    private val notificationHelper: AiNotificationHelper by inject(AiNotificationHelper::class.java)

    override suspend fun doWork(): Result {
        return try {
            doWorkInternal()
        } catch (t: Throwable) {
            AiJobProgressLog.e(WORKER_LOG_TAG, "AiJobProcessorWorker crashed", t)
            val interrupted = aiJobManager.recoverInterruptedJobs()
            AiJobNotificationSync.applyRecovery(
                notificationHelper,
                draftRepository,
                jobRepository,
                interrupted
            )
            if (t is OutOfMemoryError) Result.retry() else Result.failure()
        }
    }

    private suspend fun doWorkInternal(): Result = coroutineScope {
        notificationHelper.ensureChannel()
        val interrupted = aiJobManager.recoverInterruptedJobs()
        if (interrupted.isNotEmpty()) {
            AiJobNotificationSync.applyRecovery(
                notificationHelper,
                draftRepository,
                jobRepository,
                interrupted
            )
        }
        var shouldRetry = false
        while (true) {
            val job = jobRepository.claimNextQueued(Clock.System.now().toEpochMilliseconds()) ?: break
            val draft = draftRepository.getById(job.workspaceDraftId)
            val title = draft?.displayTitle ?: "AI processing"
            val jobId = job.id

            val initial = jobRepository.getById(jobId) ?: continue
            safeSetForeground(
                createForegroundInfo(
                    title = title,
                    notification = notificationHelper.buildProgressNotification(initial, title)
                )
            )

            val progressTicker = async {
                while (isActive) {
                    delay(PROGRESS_POLL_INTERVAL_MS)
                    val latest = jobRepository.getById(jobId) ?: break
                    if (latest.status !in ACTIVE_JOB_STATUSES) break
                    safeSetForeground(
                        createForegroundInfo(
                            title = title,
                            notification = notificationHelper.buildProgressNotification(latest, title)
                        )
                    )
                    runCatching { refreshNotificationGroup() }
                }
            }

            val success = try {
                runner.runClaimedJob(job)
            } finally {
                progressTicker.cancel()
            }

            val latest = jobRepository.getById(jobId) ?: continue
            when (latest.status) {
                AiJobStatus.COMPLETED -> notificationHelper.showCompleted(latest, title, job.workspaceDraftId)
                AiJobStatus.FAILED_FINAL,
                AiJobStatus.FAILED_RETRYABLE -> notificationHelper.showFailed(latest, title, job.workspaceDraftId)
                AiJobStatus.CANCELLED -> notificationHelper.cancel(jobId)
                else -> Unit
            }
            refreshNotificationGroup()
            if (!success && latest.status == AiJobStatus.FAILED_RETRYABLE) {
                shouldRetry = true
            }
        }
        if (shouldRetry) Result.retry() else Result.success()
    }

    private suspend fun safeSetForeground(foregroundInfo: ForegroundInfo) {
        runCatching { setForeground(foregroundInfo) }
            .onFailure { error ->
                AiJobProgressLog.w(
                    WORKER_LOG_TAG,
                    "setForeground skipped: ${error.message}",
                    error
                )
            }
    }

    private fun createForegroundInfo(
        title: String,
        notification: android.app.Notification
    ): ForegroundInfo {
        notificationHelper.ensureChannel()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private suspend fun refreshNotificationGroup() {
        val since = Clock.System.now().toEpochMilliseconds() -
            AiNotificationHelper.TERMINAL_GROUPING_WINDOW_MS
        val jobs = jobRepository.getForNotificationGrouping(since)
        val titles = jobs
            .map { it.workspaceDraftId }
            .distinct()
            .associateWith { draftId ->
                draftRepository.getById(draftId)?.displayTitle ?: "AI processing"
            }
        notificationHelper.syncGroupSummary(jobs, titles)
    }

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 7101
        private const val WORKER_LOG_TAG = "AiJobProcessorWorker"
        private const val PROGRESS_POLL_INTERVAL_MS = 1_500L

        private val ACTIVE_JOB_STATUSES = setOf(
            AiJobStatus.RUNNING,
            AiJobStatus.CHECKPOINTED,
            AiJobStatus.WAITING_FOR_NETWORK
        )
    }
}
