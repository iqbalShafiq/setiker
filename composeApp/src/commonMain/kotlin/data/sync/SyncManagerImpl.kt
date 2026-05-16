package data.sync

import data.auth.AuthManager
import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.database.SyncOperationDao
import data.local.entity.PendingSyncOperationEntity
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.remote.CloudStickerRepository
import data.remote.model.CloudStickerPack
import data.remote.model.CreateStickerPackRequest
import data.repository.createPackSyncOperation
import data.storage.StickerFileStorage
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncOperationType
import domain.model.SyncReport
import domain.model.SyncResult
import domain.model.SyncStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SYNC_STATE_SYNCED = "SYNCED"

@OptIn(ExperimentalUuidApi::class)
class SyncManagerImpl(
    private val operationDao: SyncOperationDao,
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val fileStorage: StickerFileStorage,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val networkMonitor: NetworkMonitor,
    private val syncCursorStore: SyncCursorStore,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
 ) : SyncManager {
     
      private val json = Json { ignoreUnknownKeys = true }
      private var monitoringJob: kotlinx.coroutines.Job? = null
      private val syncRunGate = SyncRunGate()
     
     override val operationsFlow: Flow<List<SyncOperation>> = 
         operationDao.observeAll().map { entities -> entities.map { it.toDomainModel() } }
     
     private val _activeOperationFlow = MutableStateFlow<SyncOperation?>(null)
     override val activeOperationFlow: StateFlow<SyncOperation?> = _activeOperationFlow
     
     private val _isSyncing = MutableStateFlow(false)
     override val isSyncing: StateFlow<Boolean> = _isSyncing
     
     private val _lastSyncReport = MutableStateFlow<SyncReport?>(null)
     override val lastSyncReport: StateFlow<SyncReport?> = _lastSyncReport

     private val _syncStage = MutableStateFlow(SyncStage.IDLE)
      override val syncStage: StateFlow<SyncStage> = _syncStage
      
      init {
         coroutineScope.launch {
             val createdOperations = reconcileLocalOnlyPacks()
             if (shouldProcessAfterReconciliation(createdOperations, networkMonitor.isOnline.value, hasSyncCredentials())) {
                 processQueue()
             }
         }
         startMonitoring()
      }
     
      override fun startMonitoring() {
          if (monitoringJob?.isActive == true) return
          networkMonitor.startMonitoring()
          monitoringJob = coroutineScope.launch {
              launch {
                  networkMonitor.isOnline.collect { isOnline ->
                      if (isOnline && hasSyncCredentials()) {
                          processQueue()
                      }
                  }
              }
              launch {
                  authManager.authState.collect {
                      if (shouldProcessAfterAuthChange(networkMonitor.isOnline.value, hasSyncCredentials())) {
                          processQueue()
                      }
                  }
              }
          }
      }
       
      override fun stopMonitoring() {
          monitoringJob?.cancel()
          monitoringJob = null
          networkMonitor.stopMonitoring()
      }
    
    override suspend fun enqueue(operation: SyncOperation) {
        operationDao.insert(operation.toEntity())
        if (networkMonitor.isOnline.value && hasSyncCredentials()) {
            processQueue()
        }
    }
    
     override suspend fun processQueue(): SyncReport {
        if (!syncRunGate.tryEnter()) return SyncReport(result = SyncResult.SkippedInProgress)
        if (!hasSyncCredentials()) {
            _syncStage.value = SyncStage.SIGN_IN_REQUIRED
            syncRunGate.leave()
            return SyncReport(result = SyncResult.SkippedNotAuthenticated, stage = SyncStage.SIGN_IN_REQUIRED)
                .also { _lastSyncReport.value = it }
        }
        if (!networkMonitor.isOnline.value) {
            _syncStage.value = SyncStage.WAITING_FOR_INTERNET
            syncRunGate.leave()
            return SyncReport(result = SyncResult.SkippedOffline, stage = SyncStage.WAITING_FOR_INTERNET)
                .also { _lastSyncReport.value = it }
        }
        
        _isSyncing.value = true
        val report = SyncReport()
         
        try {
            reconcileLocalOnlyPacks()
            _syncStage.value = SyncStage.PUSHING_LOCAL
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
                        operationDao.markCompleted(operation.id, SyncOperationStatus.SUCCESS.name, Clock.System.now().toEpochMilliseconds())
                        successCount++
                    }
                    is OperationResult.RetryableError -> {
                        if (operation.retryCount < 5) {
                            operationDao.markRetryableFailure(operation.id, result.error)
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

            _syncStage.value = SyncStage.PULLING_REMOTE
            val pullResult = pullRemoteChanges()

            val finalReport = report.copy(
                operationsProcessed = processedCount,
                operationsSucceeded = successCount,
                operationsFailed = failedCount,
                packsSynced = pullResult.packsDownloaded,
                stickersSynced = pullResult.stickersDownloaded,
                remotePacksDownloaded = pullResult.packsDownloaded,
                remoteStickersDownloaded = pullResult.stickersDownloaded,
                remoteItemsDeleted = pullResult.itemsDeleted,
                remoteDownloadFailures = pullResult.downloadFailures,
                stage = SyncStage.IDLE,
                result = if (failedCount > 0) SyncResult.Failed("$failedCount operations failed") else SyncResult.Success
            )
            _lastSyncReport.value = finalReport
            return finalReport
        } catch (e: Exception) {
            val finalReport = report.copy(
                result = SyncResult.Failed(e.message ?: "Pull sync failed"),
                stage = _syncStage.value,
            )
            _lastSyncReport.value = finalReport
            return finalReport
        } finally {
            _isSyncing.value = false
            _activeOperationFlow.value = null
            _syncStage.value = SyncStage.IDLE
            syncRunGate.leave()
        }
    }

    private suspend fun hasSyncCredentials(): Boolean = !authManager.getAccessToken().isNullOrBlank()

    private suspend fun reconcileLocalOnlyPacks(): Int {
        val blockingOperations = operationDao.getBlockingOperations()
        var createdOperations = 0
        packDao.getUnsynced().forEach { pack ->
            val cloudId = pack.cloudId
            val hasLocalOperation = hasBlockingLocalOperation(blockingOperations, pack.identifier)
            val hasCloudOperation = cloudId != null && hasBlockingLocalOperation(blockingOperations, cloudId)
            if (hasLocalOperation || hasCloudOperation) return@forEach

            operationDao.insert(
                createPackSyncOperation(
                    pack = pack,
                    stickers = stickerDao.getByPackId(pack.identifier),
                    id = Uuid.random().toString(),
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                ).toEntity()
            )
            createdOperations += 1
        }
        return createdOperations
    }

    private suspend fun pullRemoteChanges(): PullSyncResult {
        val syncData = cloudRepo.sync(syncCursorStore.getLastPullSyncAt())
        val blockingOperations = operationDao.getBlockingOperations()
        var packsDownloaded = 0
        var stickersDownloaded = 0
        var itemsDeleted = 0
        var downloadFailures = 0

        val packDelta = syncData.stickerPacks
        val remotePacks = (packDelta?.created.orEmpty() + packDelta?.updated.orEmpty())
            .distinctBy { it.id }
        for (remotePack in remotePacks) {
            if (hasBlockingLocalOperation(blockingOperations, remotePack.id)) continue
            val result = importRemotePack(remotePack)
            packsDownloaded += 1
            stickersDownloaded += result.stickersImported
            downloadFailures += result.downloadFailures
        }

        for (deletedPack in packDelta?.deleted.orEmpty()) {
            if (hasBlockingLocalOperation(blockingOperations, deletedPack.id)) continue
            val localPack = packDao.getByCloudId(deletedPack.id) ?: continue
            deleteLocalPackFiles(localPack.identifier)
            packDao.delete(localPack)
            itemsDeleted += 1
        }

        for (deletedSticker in syncData.stickers?.deleted.orEmpty()) {
            if (hasBlockingLocalOperation(blockingOperations, deletedSticker.id)) continue
            stickerDao.getByCloudId(deletedSticker.id)?.let { sticker ->
                deleteStickerFiles(sticker)
                stickerDao.deleteByCloudId(deletedSticker.id)
                itemsDeleted += 1
            }
        }

        syncData.syncToken?.let { token ->
            decodeSyncTokenToEpochMillis(token)?.let { syncCursorStore.setLastPullSyncAt(it) }
        }

        return PullSyncResult(
            packsDownloaded = packsDownloaded,
            stickersDownloaded = stickersDownloaded,
            itemsDeleted = itemsDeleted,
            downloadFailures = downloadFailures,
        )
    }

    private suspend fun importRemotePack(remotePack: CloudStickerPack): RemotePackImportResult {
        val existing = packDao.getByCloudId(remotePack.id)
        val localIdentifier = existing?.identifier ?: Uuid.random().toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val importedStickers = mutableListOf<StickerEntity>()
        var downloadFailures = 0

        remotePack.stickers
            .sortedBy { it.order }
            .forEachIndexed { index, relation ->
                val sticker = relation.sticker ?: return@forEachIndexed
                val fileName = remoteStickerFileName(sticker.id, sticker.filename ?: sticker.url)
                val localPath = runCatching {
                    val bytes = cloudRepo.downloadBytes(sticker.url)
                    fileStorage.saveBytes(bytes, fileName)
                }.getOrElse {
                    downloadFailures += 1
                    null
                } ?: return@forEachIndexed

                importedStickers += StickerEntity(
                    id = stickerDao.getByCloudId(sticker.id)?.id ?: Uuid.random().toString(),
                    packId = localIdentifier,
                    imageFile = localPath,
                    sourceImageFile = null,
                    emojis = "[]",
                    accessibilityText = sticker.name,
                    sortOrder = index,
                    cloudId = sticker.id,
                    syncState = SYNC_STATE_SYNCED,
                    lastSyncAt = now,
                    cloudUrl = sticker.url,
                )
            }

        val trayImageFile = importedStickers.firstOrNull()?.imageFile
            ?: existing?.trayImageFile
            ?: ""
        val publisher = remotePack.owner?.displayName?.takeIf { it.isNotBlank() }
            ?: remotePack.owner?.username?.takeIf { it.isNotBlank() }
            ?: "Setiker"

        val mergedPack = StickerPackEntity(
            identifier = localIdentifier,
            name = remotePack.name,
            publisher = publisher,
            trayImageFile = trayImageFile,
            isAnimated = existing?.isAnimated ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            cloudId = remotePack.id,
            syncState = SYNC_STATE_SYNCED,
            lastSyncAt = now,
            visibility = remotePack.visibility,
            cloudOwnerId = remotePack.ownerId,
        )

        if (existing == null) {
            packDao.insert(mergedPack)
        } else {
            packDao.update(mergedPack)
        }

        if (remotePack.stickers.isEmpty() || importedStickers.isNotEmpty()) {
            stickerDao.deleteByPackId(localIdentifier)
            importedStickers.forEach { stickerDao.insert(it) }
        }

        return RemotePackImportResult(
            stickersImported = importedStickers.size,
            downloadFailures = downloadFailures,
        )
    }

    private suspend fun deleteLocalPackFiles(packId: String) {
        stickerDao.getByPackId(packId).forEach { deleteStickerFiles(it) }
    }

    private suspend fun deleteStickerFiles(sticker: StickerEntity) {
        listOfNotNull(sticker.imageFile, sticker.sourceImageFile).distinct().forEach { path ->
            fileStorage.deleteImage(path)
        }
    }

    private fun remoteStickerFileName(stickerId: String, source: String): String {
        val extension = source.substringBefore('?').substringAfterLast('.', "webp")
            .takeIf { it.length in 2..5 }
            ?: "webp"
        return "cloud_${stickerId}.$extension"
    }
    
    private suspend fun executeOperation(operation: SyncOperation): OperationResult {
        return try {
            when (operation.type) {
                SyncOperationType.CREATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    val upload = cloudRepo.uploadPack(request)
                    val cloudPackId = upload.stickerPackId
                        ?: return OperationResult.PermanentError("Upload did not return sticker pack id")
                    packDao.updateCloudId(operation.targetId, cloudPackId)
                    packDao.updateSyncStatus(operation.targetId, SYNC_STATE_SYNCED, Clock.System.now().toEpochMilliseconds())
                    updateLocalStickerCloudInfo(operation.targetId, upload.stickers)
                    OperationResult.Success
                }
                SyncOperationType.UPDATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    val upload = cloudRepo.uploadPack(request, stickerPackId = operation.targetId)
                    packDao.getByCloudId(operation.targetId)?.let { pack ->
                        packDao.updateSyncStatus(pack.identifier, SYNC_STATE_SYNCED, Clock.System.now().toEpochMilliseconds())
                        updateLocalStickerCloudInfo(pack.identifier, upload.stickers)
                    }
                    OperationResult.Success
                }
                SyncOperationType.DELETE_PACK -> {
                    cloudRepo.deletePackViaUpload(operation.targetId)
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

    private suspend fun updateLocalStickerCloudInfo(packId: String, cloudStickers: List<data.remote.model.CloudSticker>) {
        val now = Clock.System.now().toEpochMilliseconds()
        val localStickers = stickerDao.getByPackId(packId).sortedBy { it.sortOrder }
        localStickers.zip(cloudStickers).forEach { (localSticker, cloudSticker) ->
            stickerDao.updateCloudInfo(localSticker.id, cloudSticker.id, cloudSticker.url)
            stickerDao.updateSyncStatus(localSticker.id, SYNC_STATE_SYNCED, now)
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
        val oneWeekAgo = Clock.System.now().toEpochMilliseconds() - (7 * 24 * 60 * 60 * 1000)
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

private data class PullSyncResult(
    val packsDownloaded: Int,
    val stickersDownloaded: Int,
    val itemsDeleted: Int,
    val downloadFailures: Int,
)

private data class RemotePackImportResult(
    val stickersImported: Int,
    val downloadFailures: Int,
)
