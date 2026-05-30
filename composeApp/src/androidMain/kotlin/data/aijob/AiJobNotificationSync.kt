package data.aijob

import domain.model.aijob.AiJob
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import kotlin.time.Clock

object AiJobNotificationSync {
    suspend fun applyRecovery(
        notificationHelper: AiNotificationHelper,
        draftRepository: WorkspaceDraftRepository,
        jobRepository: AiJobRepository,
        interrupted: List<AiJob>
    ) {
        notificationHelper.ensureChannel()
        interrupted.forEach { job ->
            notificationHelper.cancel(job.id)
            val title = draftRepository.getById(job.workspaceDraftId)?.displayTitle ?: "AI processing"
            val latest = jobRepository.getById(job.id) ?: job
            notificationHelper.showFailed(latest, title, job.workspaceDraftId)
        }
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
}
