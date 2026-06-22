package data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import data.local.entity.PendingSyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingSyncOperationEntity)

    @Update
    suspend fun update(operation: PendingSyncOperationEntity)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY priority DESC, createdAt ASC")
    suspend fun getPending(): List<PendingSyncOperationEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'IN_PROGRESS', 'FAILED')")
    suspend fun getBlockingOperations(): List<PendingSyncOperationEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'IN_PROGRESS', 'FAILED') ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingSyncOperationEntity>>

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: String): PendingSyncOperationEntity?

    @Query("UPDATE sync_queue SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: String, status: String, completedAt: Long)

    @Query("UPDATE sync_queue SET status = 'FAILED', errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)

    @Query("UPDATE sync_queue SET errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markRetryableFailure(id: String, errorMessage: String)

    @Query("DELETE FROM sync_queue WHERE status = 'SUCCESS' AND completedAt < :beforeTimestamp")
    suspend fun deleteCompletedBefore(beforeTimestamp: Long)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'IN_PROGRESS')")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()

    @Query(
        """
        UPDATE sync_queue
        SET status = 'CANCELLED'
        WHERE status = 'PENDING'
          AND type = 'CREATE_PACK'
          AND targetId = :localPackId
        """
    )
    suspend fun cancelPendingCreatePack(localPackId: String)

    @Query(
        """
        UPDATE sync_queue
        SET status = 'CANCELLED'
        WHERE status = 'PENDING'
          AND type = 'UPDATE_PACK'
          AND targetId = :cloudPackId
        """
    )
    suspend fun cancelPendingUpdatePack(cloudPackId: String)
}
