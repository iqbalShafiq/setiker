package data.aijob

import data.local.database.WorkspaceDraftDao
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftStatus
import domain.repository.WorkspaceDraftRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkspaceDraftRepository(
    private val dao: WorkspaceDraftDao
) : WorkspaceDraftRepository {
    override fun observeAll(): Flow<List<WorkspaceDraft>> =
        dao.observeAll().map { drafts -> drafts.map { it.toDomain() } }

    override fun observeById(id: String): Flow<WorkspaceDraft?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): WorkspaceDraft? = dao.getById(id)?.toDomain()

    override suspend fun upsert(draft: WorkspaceDraft) {
        dao.insert(draft.toEntity())
    }

    override suspend fun updateStatus(id: String, status: WorkspaceDraftStatus) {
        dao.updateStatus(id, status.name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun updateJobLinks(
        id: String,
        lastJobId: String?,
        lastCompletedJobId: String?,
        lastFailedJobId: String?
    ) {
        dao.updateJobLinks(
            id = id,
            lastJobId = lastJobId,
            lastCompletedJobId = lastCompletedJobId,
            lastFailedJobId = lastFailedJobId,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun deleteOlderThan(beforeTimestamp: Long) {
        dao.deleteOlderThan(beforeTimestamp)
    }
}
