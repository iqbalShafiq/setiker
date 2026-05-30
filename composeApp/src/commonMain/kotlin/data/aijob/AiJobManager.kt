package data.aijob

import domain.model.aijob.AiJob
import domain.model.aijob.AiJobFailureKind
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftStatus
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalUuidApi::class)
class AiJobManager(
    private val jobRepository: AiJobRepository,
    private val draftRepository: WorkspaceDraftRepository,
    private val scheduler: AiBackgroundScheduler
) {
    companion object {
        const val MAX_AUTO_RETRY_ATTEMPTS = 5
        private const val BASE_BACKOFF_MS = 15_000L
    }

    fun observeJobs(): Flow<List<AiJob>> = jobRepository.observeAll()

    fun observeDrafts(): Flow<List<WorkspaceDraft>> = draftRepository.observeAll()

    fun observeActiveJobCount(): Flow<Int> = jobRepository.observeActiveCount()

    suspend fun getDraft(id: String): WorkspaceDraft? = draftRepository.getById(id)

    suspend fun upsertDraft(draft: WorkspaceDraft) {
        draftRepository.upsert(draft)
    }

    suspend fun enqueue(
        draft: WorkspaceDraft,
        type: AiJobType,
        origin: AiJobOrigin,
        payloadJson: String,
        requiresNetwork: Boolean,
        parentJobId: String? = null,
        attemptGroupId: String? = null
    ): AiJob {
        draftRepository.upsert(
            draft.copy(
                status = WorkspaceDraftStatus.PROCESSING,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        val existingActive = jobRepository.getActiveJobForDraft(draft.id)
        if (existingActive != null) {
            return existingActive
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val groupId = attemptGroupId ?: Uuid.random().toString()
        val attemptCount = if (parentJobId != null) {
            (jobRepository.getById(parentJobId)?.attemptCount ?: 0) + 1
        } else {
            1
        }
        val job = AiJob(
            id = Uuid.random().toString(),
            workspaceDraftId = draft.id,
            type = type,
            status = AiJobStatus.QUEUED,
            origin = origin,
            payloadJson = payloadJson,
            attemptCount = attemptCount,
            attemptGroupId = groupId,
            parentJobId = parentJobId,
            requiresNetwork = requiresNetwork,
            createdAt = now,
            updatedAt = now
        )
        jobRepository.insert(job)
        draftRepository.updateJobLinks(draft.id, lastJobId = job.id)
        scheduler.scheduleJob(job.id, requiresNetwork)
        return job
    }

    suspend fun retryDraft(draftId: String): AiJob? {
        val draft = draftRepository.getById(draftId) ?: return null
        val failedJob = draft.lastFailedJobId?.let { jobRepository.getById(it) }
        val template = failedJob ?: draft.lastJobId?.let { jobRepository.getById(it) } ?: return null
        return enqueue(
            draft = draft.copy(
                status = WorkspaceDraftStatus.PROCESSING,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            ),
            type = template.type,
            origin = template.origin,
            payloadJson = template.payloadJson,
            requiresNetwork = template.requiresNetwork,
            parentJobId = template.id,
            attemptGroupId = template.attemptGroupId
        )
    }

    suspend fun cancelJob(jobId: String) {
        val job = jobRepository.getById(jobId) ?: return
        jobRepository.updateStatus(
            id = jobId,
            status = AiJobStatus.CANCEL_REQUESTED,
            failureKind = AiJobFailureKind.CANCELLED,
            failureMessage = "Cancelled by user"
        )
        scheduler.cancelJob(jobId)
        jobRepository.updateStatus(
            id = jobId,
            status = AiJobStatus.CANCELLED,
            failureKind = AiJobFailureKind.CANCELLED,
            failureMessage = "Cancelled by user",
            completedAt = Clock.System.now().toEpochMilliseconds()
        )
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.CANCELLED)
    }

    suspend fun deleteDraft(draftId: String) {
        val draft = draftRepository.getById(draftId) ?: return
        draft.lastJobId?.let { jobRepository.deleteJob(it) }
        draftRepository.delete(draftId)
    }

    suspend fun recoverInterruptedJobs(): List<AiJob> {
        val now = Clock.System.now().toEpochMilliseconds()
        jobRepository.requeueActiveRunningJobs(now)
        val interrupted = jobRepository.resetStaleRunningJobs(now)
        interrupted.forEach { job ->
            draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.NEEDS_ATTENTION)
            draftRepository.updateJobLinks(
                id = job.workspaceDraftId,
                lastFailedJobId = job.id
            )
        }
        return interrupted
    }

    suspend fun resumeOnStartup(): List<AiJob> {
        val interrupted = recoverInterruptedJobs()
        scheduler.resumeQueuedJobs()
        return interrupted
    }

    fun computeNextRetryAt(attemptCount: Int): Long {
        val multiplier = 1 shl attemptCount.coerceAtMost(4)
        val jitter = Random.nextLong(0, 5_000L)
        return Clock.System.now().toEpochMilliseconds() + BASE_BACKOFF_MS * multiplier + jitter
    }
}
