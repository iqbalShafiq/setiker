package data.aijob

import data.local.database.AiJobDao
import domain.model.aijob.AiJob
import domain.model.aijob.AiJobFailureKind
import domain.model.aijob.AiJobProgress
import domain.model.aijob.AiJobStatus
import domain.repository.AiJobRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAiJobRepository(
    private val dao: AiJobDao
) : AiJobRepository {
    companion object {
        /** Jobs without progress heartbeat longer than this are treated as crashed. */
        const val STALE_RUNNING_THRESHOLD_MS = 15 * 60 * 1000L
    }
    override fun observeAll(): Flow<List<AiJob>> =
        dao.observeAll().map { jobs -> jobs.map { it.toDomain() } }

    override fun observeByDraft(workspaceDraftId: String): Flow<List<AiJob>> =
        dao.observeByDraft(workspaceDraftId).map { jobs -> jobs.map { it.toDomain() } }

    override fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()

    override suspend fun getById(id: String): AiJob? = dao.getById(id)?.toDomain()

    override suspend fun insert(job: AiJob) {
        dao.insert(job.toEntity())
    }

    override suspend fun update(job: AiJob) {
        dao.update(job.toEntity())
    }

    override suspend fun claimNextQueued(nowEpochMillis: Long): AiJob? {
        val next = dao.getNextQueued(nowEpochMillis) ?: return null
        val claimed = dao.markRunning(next.id, nowEpochMillis)
        if (claimed == 0) return null
        return dao.getById(next.id)?.toDomain()
    }

    override suspend fun requeueActiveRunningJobs(nowEpochMillis: Long): Int {
        val staleBefore = nowEpochMillis - STALE_RUNNING_THRESHOLD_MS
        return dao.requeueActiveRunningJobs(
            activeSinceEpochMillis = staleBefore,
            nowEpochMillis = nowEpochMillis
        )
    }

    override suspend fun resetStaleRunningJobs(nowEpochMillis: Long): List<AiJob> {
        val staleBefore = nowEpochMillis - STALE_RUNNING_THRESHOLD_MS
        val interrupted = dao.getStaleRunningJobs(staleBefore).map { it.toDomain() }
        if (interrupted.isEmpty()) return emptyList()
        dao.resetStaleRunning(
            staleBeforeEpochMillis = staleBefore,
            nowEpochMillis = nowEpochMillis
        )
        return interrupted.map { job ->
            dao.getById(job.id)?.toDomain() ?: job.copy(
                status = AiJobStatus.FAILED_RETRYABLE,
                failureMessage = "Processing was interrupted"
            )
        }
    }

    override suspend fun getActiveJobForDraft(workspaceDraftId: String): AiJob? =
        dao.getActiveForDraft(workspaceDraftId)?.toDomain()

    override suspend fun updateProgress(id: String, progress: AiJobProgress) {
        dao.updateProgress(
            id = id,
            stepKey = progress.stepKey,
            stepLabel = progress.stepLabel,
            fraction = progress.fraction,
            nowEpochMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateStatus(
        id: String,
        status: AiJobStatus,
        failureKind: AiJobFailureKind,
        failureMessage: String?,
        resultJson: String?,
        checkpointJson: String?,
        completedAt: Long?
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateStatus(
            id = id,
            status = status.name,
            failureKind = failureKind.name,
            failureMessage = failureMessage,
            resultJson = resultJson,
            checkpointJson = checkpointJson,
            completedAt = completedAt,
            nowEpochMillis = now
        )
    }

    override suspend fun deleteJob(id: String) {
        dao.deleteById(id)
    }

    override suspend fun deleteCompletedBefore(beforeTimestamp: Long) {
        dao.deleteCompletedBefore(beforeTimestamp)
    }

    override suspend fun getForNotificationGrouping(terminalSinceEpochMillis: Long): List<AiJob> =
        dao.getForNotificationGrouping(terminalSinceEpochMillis).map { it.toDomain() }
}
