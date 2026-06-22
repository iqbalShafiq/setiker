package data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.local.entity.ProcessingHistoryCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessingHistoryCacheDao {
    @Query("SELECT * FROM processing_history_cache ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProcessingHistoryCacheEntity>>

    @Query("SELECT * FROM processing_history_cache WHERE (:type IS NULL OR type = :type) ORDER BY createdAt DESC")
    fun observeByType(type: String?): Flow<List<ProcessingHistoryCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(items: List<ProcessingHistoryCacheEntity>)

    @Query("DELETE FROM processing_history_cache")
    suspend fun clear()
}
