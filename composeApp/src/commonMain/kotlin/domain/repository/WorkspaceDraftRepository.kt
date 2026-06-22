package domain.repository

import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftStatus
import kotlinx.coroutines.flow.Flow

interface WorkspaceDraftRepository {
    fun observeAll(): Flow<List<WorkspaceDraft>>
    fun observeById(id: String): Flow<WorkspaceDraft?>
    suspend fun getById(id: String): WorkspaceDraft?
    suspend fun upsert(draft: WorkspaceDraft)
    suspend fun updateStatus(id: String, status: WorkspaceDraftStatus)
    suspend fun updateJobLinks(
        id: String,
        lastJobId: String? = null,
        lastCompletedJobId: String? = null,
        lastFailedJobId: String? = null
    )
    suspend fun delete(id: String)
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
