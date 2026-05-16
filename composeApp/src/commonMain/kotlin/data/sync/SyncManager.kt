package data.sync

import domain.model.SyncOperation
import domain.model.SyncReport
import domain.model.SyncStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncManager {
    val operationsFlow: Flow<List<SyncOperation>>
    val activeOperationFlow: StateFlow<SyncOperation?>
    val isSyncing: StateFlow<Boolean>
    val lastSyncReport: StateFlow<SyncReport?>
    val syncStage: StateFlow<SyncStage>
    
    suspend fun enqueue(operation: SyncOperation)
    suspend fun processQueue(): SyncReport
    suspend fun retry(operationId: String)
    suspend fun cancel(operationId: String)
    suspend fun clearCompleted()
    suspend fun sync(): SyncReport
    fun startMonitoring()
    fun stopMonitoring()
}
