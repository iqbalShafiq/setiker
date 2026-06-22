package data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import data.local.entity.WorkspaceDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draft: WorkspaceDraftEntity)

    @Update
    suspend fun update(draft: WorkspaceDraftEntity)

    @Query("SELECT * FROM workspace_drafts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WorkspaceDraftEntity>>

    @Query("SELECT * FROM workspace_drafts WHERE id = :id")
    fun observeById(id: String): Flow<WorkspaceDraftEntity?>

    @Query("SELECT * FROM workspace_drafts WHERE id = :id")
    suspend fun getById(id: String): WorkspaceDraftEntity?

    @Query("UPDATE workspace_drafts SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query(
        """
        UPDATE workspace_drafts SET
            lastJobId = COALESCE(:lastJobId, lastJobId),
            lastCompletedJobId = COALESCE(:lastCompletedJobId, lastCompletedJobId),
            lastFailedJobId = COALESCE(:lastFailedJobId, lastFailedJobId),
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateJobLinks(
        id: String,
        lastJobId: String?,
        lastCompletedJobId: String?,
        lastFailedJobId: String?,
        updatedAt: Long
    )

    @Query("DELETE FROM workspace_drafts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM workspace_drafts WHERE updatedAt < :beforeTimestamp AND status IN ('APPLIED', 'CANCELLED')")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
