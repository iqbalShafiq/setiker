package data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import data.local.entity.AiJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: AiJobEntity)

    @Update
    suspend fun update(job: AiJobEntity)

    @Query("SELECT * FROM ai_jobs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AiJobEntity>>

    @Query("SELECT * FROM ai_jobs WHERE workspaceDraftId = :draftId ORDER BY createdAt DESC")
    fun observeByDraft(draftId: String): Flow<List<AiJobEntity>>

    @Query("SELECT COUNT(*) FROM ai_jobs WHERE status IN ('QUEUED', 'RUNNING', 'WAITING_FOR_NETWORK', 'CHECKPOINTED', 'CANCEL_REQUESTED')")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM ai_jobs WHERE id = :id")
    suspend fun getById(id: String): AiJobEntity?

    @Query(
        """
        SELECT * FROM ai_jobs
        WHERE status = 'QUEUED'
        AND (nextRetryAt IS NULL OR nextRetryAt <= :nowEpochMillis)
        ORDER BY createdAt ASC
        LIMIT 1
        """
    )
    suspend fun getNextQueued(nowEpochMillis: Long): AiJobEntity?

    @Query(
        """
        UPDATE ai_jobs SET status = 'RUNNING', updatedAt = :nowEpochMillis, lastAttemptAt = :nowEpochMillis
        WHERE id = :id AND status = 'QUEUED'
        """
    )
    suspend fun markRunning(id: String, nowEpochMillis: Long): Int

    @Query(
        """
        SELECT * FROM ai_jobs
        WHERE workspaceDraftId = :draftId
        AND status IN ('QUEUED', 'RUNNING', 'WAITING_FOR_NETWORK', 'CHECKPOINTED', 'CANCEL_REQUESTED')
        LIMIT 1
        """
    )
    suspend fun getActiveForDraft(draftId: String): AiJobEntity?

    @Query(
        """
        SELECT * FROM ai_jobs
        WHERE status IN ('RUNNING', 'CHECKPOINTED', 'WAITING_FOR_NETWORK')
        AND updatedAt < :staleBeforeEpochMillis
        """
    )
    suspend fun getStaleRunningJobs(staleBeforeEpochMillis: Long): List<AiJobEntity>

    @Query(
        """
        UPDATE ai_jobs
        SET status = 'QUEUED',
            failureKind = 'NONE',
            failureMessage = NULL,
            completedAt = NULL,
            updatedAt = :nowEpochMillis
        WHERE status IN ('RUNNING', 'CHECKPOINTED', 'WAITING_FOR_NETWORK')
        AND updatedAt >= :activeSinceEpochMillis
        """
    )
    suspend fun requeueActiveRunningJobs(
        activeSinceEpochMillis: Long,
        nowEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE ai_jobs
        SET status = 'FAILED_RETRYABLE',
            failureKind = 'UNKNOWN',
            failureMessage = 'Processing was interrupted',
            updatedAt = :nowEpochMillis,
            completedAt = :nowEpochMillis
        WHERE status IN ('RUNNING', 'CHECKPOINTED', 'WAITING_FOR_NETWORK')
        AND updatedAt < :staleBeforeEpochMillis
        """
    )
    suspend fun resetStaleRunning(
        staleBeforeEpochMillis: Long,
        nowEpochMillis: Long
    )

    @Query(
        """
        UPDATE ai_jobs SET
            progressStepKey = :stepKey,
            progressStepLabel = :stepLabel,
            progressFraction = :fraction,
            updatedAt = :nowEpochMillis
        WHERE id = :id
        """
    )
    suspend fun updateProgress(
        id: String,
        stepKey: String,
        stepLabel: String,
        fraction: Float,
        nowEpochMillis: Long
    )

    @Query(
        """
        UPDATE ai_jobs SET
            status = :status,
            failureKind = :failureKind,
            failureMessage = :failureMessage,
            resultJson = :resultJson,
            checkpointJson = :checkpointJson,
            completedAt = :completedAt,
            updatedAt = :nowEpochMillis
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: String,
        status: String,
        failureKind: String,
        failureMessage: String?,
        resultJson: String?,
        checkpointJson: String?,
        completedAt: Long?,
        nowEpochMillis: Long
    )

    @Query("DELETE FROM ai_jobs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ai_jobs WHERE status IN ('COMPLETED', 'APPLIED', 'CANCELLED', 'FAILED_FINAL') AND completedAt IS NOT NULL AND completedAt < :beforeTimestamp")
    suspend fun deleteCompletedBefore(beforeTimestamp: Long)

    @Query(
        """
        SELECT * FROM ai_jobs
        WHERE status IN ('QUEUED', 'RUNNING', 'WAITING_FOR_NETWORK', 'CHECKPOINTED', 'CANCEL_REQUESTED')
        OR (
            status IN ('COMPLETED', 'FAILED_RETRYABLE', 'FAILED_FINAL')
            AND completedAt IS NOT NULL
            AND completedAt >= :terminalSinceMs
        )
        ORDER BY createdAt DESC
        LIMIT 12
        """
    )
    suspend fun getForNotificationGrouping(terminalSinceMs: Long): List<AiJobEntity>
}
