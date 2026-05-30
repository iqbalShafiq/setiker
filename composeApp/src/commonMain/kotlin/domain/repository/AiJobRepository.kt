package domain.repository

import domain.model.aijob.AiJob
import domain.model.aijob.AiJobProgress
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import kotlinx.coroutines.flow.Flow

interface AiJobRepository {
    fun observeAll(): Flow<List<AiJob>>
    fun observeByDraft(workspaceDraftId: String): Flow<List<AiJob>>
    fun observeActiveCount(): Flow<Int>
    suspend fun getById(id: String): AiJob?
    suspend fun insert(job: AiJob)
    suspend fun update(job: AiJob)
    suspend fun claimNextQueued(nowEpochMillis: Long): AiJob?
    suspend fun requeueActiveRunningJobs(nowEpochMillis: Long): Int
    suspend fun resetStaleRunningJobs(nowEpochMillis: Long): List<AiJob>
    suspend fun getActiveJobForDraft(workspaceDraftId: String): AiJob?
    suspend fun updateProgress(id: String, progress: AiJobProgress)
    suspend fun updateStatus(
        id: String,
        status: AiJobStatus,
        failureKind: domain.model.aijob.AiJobFailureKind = domain.model.aijob.AiJobFailureKind.NONE,
        failureMessage: String? = null,
        resultJson: String? = null,
        checkpointJson: String? = null,
        completedAt: Long? = null
    )
    suspend fun deleteJob(id: String)
    suspend fun deleteCompletedBefore(beforeTimestamp: Long)
    suspend fun getForNotificationGrouping(terminalSinceEpochMillis: Long): List<AiJob>
}
