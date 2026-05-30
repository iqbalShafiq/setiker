package data.aijob

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.setiker.app.MainActivity
import com.setiker.app.R
import domain.model.aijob.AiJob
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType

class AiNotificationHelper(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing?.importance == NotificationManager.IMPORTANCE_HIGH) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI Processing",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Background AI and sticker processing"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun buildProgressNotification(job: AiJob, title: String): Notification {
        val fraction = (job.progress?.fraction ?: 0f).coerceIn(0f, 1f)
        val percent = (fraction * 100).toInt().coerceIn(0, 100)
        val stepLabel = job.progress?.stepLabel ?: "Processing..."
        return childBuilder(job, title)
            .setContentTitle(title)
            .setContentText(stepLabel)
            .setStyle(NotificationCompat.BigTextStyle().bigText(stepLabel))
            .setProgress(100, percent, percent <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(createPendingIntent(job.workspaceDraftId, job.id))
            .build()
    }

    fun showProgress(job: AiJob, title: String) {
        notify(job.id.hashCode(), buildProgressNotification(job, title))
    }

    fun showCompleted(job: AiJob, title: String, draftId: String) {
        val notification = childBuilder(job, title)
            .setContentTitle(title)
            .setContentText("Selesai — ketuk untuk melihat hasil")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Selesai — ketuk untuk melihat hasil")
            )
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createPendingIntent(draftId, job.id))
            .build()
        notify(job.id.hashCode(), notification)
    }

    fun showFailed(job: AiJob, title: String, draftId: String) {
        val message = job.failureMessage ?: "Processing failed"
        val isRetryable = job.status == AiJobStatus.FAILED_RETRYABLE
        val body = if (isRetryable) {
            "$message\nKetuk untuk mencoba lagi dari AI Jobs."
        } else {
            message
        }
        val notification = childBuilder(job, title)
            .setContentTitle("$title — gagal")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createPendingIntent(draftId, job.id))
            .build()
        notify(job.id.hashCode(), notification)
    }

    /**
     * Group summary (collapsed stack / expanded list per Android notification groups).
     * @see <a href="https://developer.android.com/develop/ui/views/notifications/group">Create a group of notifications</a>
     */
    fun syncGroupSummary(jobs: List<AiJob>, titleByDraftId: Map<String, String> = emptyMap()) {
        if (!canPostNotifications()) return
        if (jobs.isEmpty()) {
            notificationManager.cancel(GROUP_SUMMARY_NOTIFICATION_ID)
            return
        }
        // Single active job uses the WorkManager foreground notification only — avoid a second summary.
        if (jobs.size <= 1) {
            notificationManager.cancel(GROUP_SUMMARY_NOTIFICATION_ID)
            return
        }
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("Setiker AI")
            .setSummaryText("${jobs.size} tugas")
        jobs.take(INBOX_LINE_LIMIT).forEach { job ->
            val title = titleByDraftId[job.workspaceDraftId] ?: defaultTitleFor(job)
            inboxStyle.addLine(summaryLineFor(job, title))
        }
        if (jobs.size > INBOX_LINE_LIMIT) {
            inboxStyle.addLine("+${jobs.size - INBOX_LINE_LIMIT} lainnya")
        }
        val summary = baseBuilder()
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setContentTitle("Setiker AI")
            .setContentText("${jobs.size} tugas AI")
            .setStyle(inboxStyle)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(createAiJobsPendingIntent())
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                }
            }
            .build()
        notify(GROUP_SUMMARY_NOTIFICATION_ID, summary)
    }

    fun cancel(jobId: String) {
        if (!canPostNotifications()) return
        notificationManager.cancel(jobId.hashCode())
    }

    private fun childBuilder(job: AiJob, title: String): NotificationCompat.Builder =
        baseBuilder()
            .setGroup(GROUP_KEY)
            .setSubText(jobTypeLabel(job.type))

    private fun summaryLineFor(job: AiJob, title: String): String {
        val fraction = (job.progress?.fraction ?: 0f).coerceIn(0f, 1f)
        val percent = (fraction * 100).toInt().coerceIn(0, 100)
        return when (job.status) {
            AiJobStatus.QUEUED -> "$title — antrian"
            AiJobStatus.RUNNING,
            AiJobStatus.CHECKPOINTED,
            AiJobStatus.WAITING_FOR_NETWORK,
            AiJobStatus.CANCEL_REQUESTED -> "$title — $percent%"
            AiJobStatus.COMPLETED -> "$title — selesai"
            AiJobStatus.FAILED_RETRYABLE,
            AiJobStatus.FAILED_FINAL -> "$title — gagal"
            AiJobStatus.CANCELLED -> "$title — dibatalkan"
            else -> title
        }
    }

    private fun defaultTitleFor(job: AiJob): String = when (job.type) {
        AiJobType.GENERATE_PACK -> "Buat pack"
        AiJobType.VIDEO_PACK -> "Video pack"
        AiJobType.ANIMATED_ENCODE -> "Encode animasi"
        AiJobType.REMOVE_BACKGROUND -> "Hapus background"
        AiJobType.GENERATE_STICKERS -> "Generate stiker"
        AiJobType.IMPROVE_STICKERS -> "Perbaiki stiker"
        AiJobType.GRID_SPLIT -> "Pisah grid"
    }

    private fun jobTypeLabel(type: AiJobType): String = when (type) {
        AiJobType.GENERATE_PACK -> "Pack"
        AiJobType.VIDEO_PACK -> "Video"
        AiJobType.ANIMATED_ENCODE -> "Animasi"
        AiJobType.REMOVE_BACKGROUND -> "Background"
        AiJobType.GENERATE_STICKERS -> "Stiker"
        AiJobType.IMPROVE_STICKERS -> "Improve"
        AiJobType.GRID_SPLIT -> "Grid"
    }

    private fun baseBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }

    private fun notify(id: Int, notification: Notification) {
        if (!canPostNotifications()) return
        try {
            notificationManager.notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — in-app AI Jobs screen still shows progress.
        }
    }

    private fun canPostNotifications(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun createPendingIntent(draftId: String, jobId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("setiker://ai/draft/$draftId?jobId=$jobId")
            putExtra(EXTRA_DRAFT_ID, draftId)
            putExtra(EXTRA_JOB_ID, jobId)
        }
        return PendingIntent.getActivity(
            context,
            (draftId + jobId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createAiJobsPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("setiker://ai/jobs")
            putExtra(EXTRA_OPEN_AI_JOBS, true)
        }
        return PendingIntent.getActivity(
            context,
            AI_JOBS_PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "ai_processing_high"
        private const val LEGACY_CHANNEL_ID = "ai_processing"
        const val EXTRA_DRAFT_ID = "extra_workspace_draft_id"
        const val EXTRA_JOB_ID = "extra_ai_job_id"
        const val EXTRA_OPEN_AI_JOBS = "extra_open_ai_jobs"
        const val PROCESSOR_WORK_NAME = "ai_job_processor"
        const val GROUP_KEY = "setiker_ai_jobs"
        const val GROUP_SUMMARY_NOTIFICATION_ID = 7100
        private const val AI_JOBS_PENDING_INTENT_REQUEST_CODE = 7102
        private const val INBOX_LINE_LIMIT = 5
        const val TERMINAL_GROUPING_WINDOW_MS = 5 * 60 * 1000L
    }
}
