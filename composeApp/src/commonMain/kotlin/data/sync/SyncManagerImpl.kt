package data.sync

import data.auth.AuthManager
import data.local.database.SyncOperationDao
import data.local.entity.PendingSyncOperationEntity
import data.remote.CloudStickerRepository
import data.remote.CreateStickerPackRequest
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncOperationType
import domain.model.SyncReport
import domain.model.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SyncManagerImpl(
    private val operationDao: SyncOperationDao,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val networkMonitor: NetworkMonitor,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SyncManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override val operationsFlow: Flow<List<SyncOperation>> = 
        operationDao.observeAll().map { entities -> entities.map { it.toDomainModel() } }
    
    private val _activeOperationFlow = MutableStateFlow<SyncOperation?>(null)
    override val activeOperationFlow: StateFlow<SyncOperation?> = _activeOperationFlow
    
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val _lastSyncReport = MutableStateFlow<SyncReport?>(null)
    override val lastSyncReport: StateFlow<SyncReport?> = _lastSyncReport
    
    init {
        startMonitoring()
    }
    
    override fun startMonitoring() {
        coroutineScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline && authManager.isAuthenticated()) {
                    processQueue()
                }
            }
        }
    }
    
    override fun stopMonitoring() {}
    
    override suspend fun enqueue(operation: SyncOperation) {
        operationDao.insert(operation.toEntity())
        if (networkMonitor.isOnline.value && authManager.isAuthenticated()) {
            processQueue()
        }
    }
    
    override suspend fun processQueue(): SyncReport {
        if (_isSyncing.value) return SyncReport(result = SyncResult.SkippedOffline)
        if (!authManager.isAuthenticated()) return SyncReport(result = SyncResult.SkippedNotAuthenticated)
        if (!networkMonitor.isOnline.value) return SyncReport(result = SyncResult.SkippedOffline)
        
        _isSyncing.value = true
        val report = SyncReport()
        
        try {
            val pendingOps = operationDao.getPending()
            var processedCount = 0
            var successCount = 0
            var failedCount = 0
            
            for (entity in pendingOps) {
                val operation = entity.toDomainModel()
                _activeOperationFlow.value = operation
                
                val result = executeOperation(operation)
                processedCount++
                
                when (result) {
                    is OperationResult.Success -> {
                        operationDao.markCompleted(operation.id, SyncOperationStatus.SUCCESS.name, System.currentTimeMillis())
                        successCount++
                    }
                    is OperationResult.RetryableError -> {
                        if (operation.retryCount < 5) {
                            operationDao.update(entity.copy(retryCount = operation.retryCount + 1))
                        } else {
                            operationDao.markFailed(operation.id, result.error)
                            failedCount++
                        }
                    }
                    is OperationResult.PermanentError -> {
                        operationDao.markFailed(operation.id, result.error)
                        failedCount++
                    }
                }
                delay(100)
            }
            
            val finalReport = report.copy(
                operationsProcessed = processedCount,
                operationsSucceeded = successCount,
                operationsFailed = failedCount,
                result = if (failedCount > 0) SyncResult.Failed("$failedCount operations failed") else SyncResult.Success
            )
            _lastSyncReport.value = finalReport
            return finalReport
        } finally {
            _isSyncing.value = false
            _activeOperationFlow.value = null
        }
    }
    
    private suspend fun executeOperation(operation: SyncOperation): OperationResult {
        return try {
            when (operation.type) {
                SyncOperationType.CREATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    cloudRepo.createPack(request)
                    OperationResult.Success
                }
                SyncOperationType.UPDATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    cloudRepo.updatePack(operation.targetId, request)
                    OperationResult.Success
                }
                SyncOperationType.DELETE_PACK -> {
                    cloudRepo.deletePack(operation.targetId)
                    OperationResult.Success
                }
                else -> OperationResult.PermanentError("Operation type ${operation.type} not implemented")
            }
        } catch (e: Exception) {
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> OperationResult.RetryableError(e.message ?: "Timeout")
                e.message?.contains("5", ignoreCase = true) == true -> OperationResult.RetryableError(e.message ?: "Server error")
                else -> OperationResult.PermanentError(e.message ?: "Unknown error")
            }
        }
    }
    
    override suspend fun retry(operationId: String) {
        val entity = operationDao.getById(operationId) ?: return
        operationDao.update(entity.copy(status = SyncOperationStatus.PENDING.name))
        processQueue()
    }
    
    override suspend fun cancel(operationId: String) {
        val entity = operationDao.getById(operationId) ?: return
        operationDao.update(entity.copy(status = SyncOperationStatus.CANCELLED.name))
    }
    
    override suspend fun clearCompleted() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        operationDao.deleteCompletedBefore(oneWeekAgo)
    }
    
    override suspend fun sync(): SyncReport = processQueue()
    
    private fun PendingSyncOperationEntity.toDomainModel() = SyncOperation(
        id = id, type = SyncOperationType.valueOf(type), targetId = targetId,
        payload = payload, status = SyncOperationStatus.valueOf(status),
        errorMessage = errorMessage, retryCount = retryCount,
        createdAt = createdAt, completedAt = completedAt, priority = priority
    )
    
    private fun SyncOperation.toEntity() = PendingSyncOperationEntity(
        id = id, type = type.name, targetId = targetId, payload = payload,
        status = status.name, errorMessage = errorMessage, retryCount = retryCount,
        createdAt = createdAt, completedAt = completedAt, priority = priority
    )
}

sealed class OperationResult {
    data object Success : OperationResult()
    data class RetryableError(val error: String) : OperationResult()
    data class PermanentError(val error: String) : OperationResult()
}
