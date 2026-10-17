package data.sync

import data.auth.AuthManager
import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.database.SyncOperationDao
import data.local.entity.PendingSyncOperationEntity
import data.remote.CloudStickerRepository
import data.storage.StickerFileStorage
import data.sync.operationBelongsToPack
import data.sync.operationIsPackVisibilityUpdate
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
import domain.error.detailText
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

/**
 * Gate, orchestration and reporting for the sync plane (spec P3b, D4). Push execution lives in
 * [SyncOperationExecutor], reconciliation in [SyncReconciler], the pull pipeline in
 * [RemotePullProcessor]; this facade owns the queue, the run gate, and `SyncReport`.
 */
class SyncManagerImpl(
    private val operationDao: SyncOperationDao,
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val fileStorage: StickerFileStorage,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val networkMonitor: NetworkMonitor,
    private val syncCursorStore: SyncCursorStore,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : SyncManager {

    private val executor = SyncOperationExecutor(packDao, stickerDao, cloudRepo, authManager, syncCursorStore)
    private val reconciler = SyncReconciler(operationDao, packDao, stickerDao, authManager)
    private val pullProcessor =
        RemotePullProcessor(operationDao, packDao, stickerDao, fileStorage, cloudRepo, authManager, syncCursorStore)

    private var monitoringJob: kotlinx.coroutines.Job? = null
    private val syncRunGate = SyncRunGate()

    override val operationsFlow: Flow<List<SyncOperation>> =
        operationDao.observeAll().map { entities -> entities.map { it.toSyncOperation() } }

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
            val createdOperations = reconciler.reconcileLocalOnlyPacks()
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
        operationDao.insert(operation.toPendingSyncEntity())
    }

    override suspend fun processQueue(): SyncReport = processQueueInternal(skipPull = false)

    override suspend fun pushPendingOperations(): SyncReport = processQueueInternal(skipPull = true)

    override suspend fun pushPackOperations(localPackId: String, cloudPackId: String?): SyncReport {
        val resolvedCloudId = cloudPackId ?: packDao.getById(localPackId)?.cloudId
        return processQueueInternal(
            skipPull = true,
            scopedLocalPackId = localPackId,
            scopedCloudPackId = resolvedCloudId,
            reconcileBeforePush = true,
            visibilityOnly = false,
        )
    }

    override suspend fun pushPackVisibilityOperations(localPackId: String, cloudPackId: String?): SyncReport {
        val resolvedCloudId = cloudPackId ?: packDao.getById(localPackId)?.cloudId
        return processQueueInternal(
            skipPull = true,
            scopedLocalPackId = localPackId,
            scopedCloudPackId = resolvedCloudId,
            reconcileBeforePush = false,
            visibilityOnly = true,
        )
    }

    override suspend fun cancelPendingContentUploadOps(localPackId: String, cloudPackId: String?) {
        operationDao.cancelPendingCreatePack(localPackId)
        if (!cloudPackId.isNullOrBlank()) {
            operationDao.cancelPendingUpdatePack(cloudPackId)
        }
    }

    private suspend fun processQueueInternal(
        skipPull: Boolean,
        scopedLocalPackId: String? = null,
        scopedCloudPackId: String? = null,
        reconcileBeforePush: Boolean = true,
        visibilityOnly: Boolean = false,
    ): SyncReport = syncRunGate.withLock {
        if (!hasSyncCredentials()) {
            _syncStage.value = SyncStage.SIGN_IN_REQUIRED
            return@withLock SyncReport(result = SyncResult.SkippedNotAuthenticated, stage = SyncStage.SIGN_IN_REQUIRED)
                .also { _lastSyncReport.value = it }
        }
        if (!networkMonitor.isOnline.value) {
            _syncStage.value = SyncStage.WAITING_FOR_INTERNET
            return@withLock SyncReport(result = SyncResult.SkippedOffline, stage = SyncStage.WAITING_FOR_INTERNET)
                .also { _lastSyncReport.value = it }
        }

        _isSyncing.value = true
        val report = SyncReport()

        try {
            if (reconcileBeforePush) {
                if (scopedLocalPackId != null) {
                    reconciler.reconcilePack(scopedLocalPackId)
                } else {
                    reconciler.reconcileLocalOnlyPacks()
                }
            }
            _syncStage.value = SyncStage.PUSHING_LOCAL
            val pendingOps = operationDao.getPending()
                .map { it.toSyncOperation() }
                .filter { operation ->
                    when {
                        visibilityOnly -> {
                            scopedLocalPackId != null &&
                                operationIsPackVisibilityUpdate(operation) &&
                                operationBelongsToPack(
                                    operation = operation,
                                    localPackId = scopedLocalPackId,
                                    cloudPackId = scopedCloudPackId,
                                )
                        }
                        scopedLocalPackId == null -> true
                        else -> operationBelongsToPack(
                            operation = operation,
                            localPackId = scopedLocalPackId,
                            cloudPackId = scopedCloudPackId,
                        )
                    }
                }
            var processedCount = 0
            var successCount = 0
            var failedCount = 0

            for (operation in pendingOps) {
                _activeOperationFlow.value = operation

                val outcome = executor.execute(operation)
                processedCount++

                when (outcome) {
                    is Result.Success -> {
                        operationDao.markCompleted(operation.id, SyncOperationStatus.SUCCESS.name, Clock.System.now().toEpochMilliseconds())
                        successCount++
                    }
                    is Result.Error -> {
                        val detail = outcome.error.detailText()
                        if (outcome.error.retryable && operation.retryCount < 5) {
                            operationDao.markRetryableFailure(operation.id, detail)
                        } else {
                            operationDao.markFailed(operation.id, detail)
                            failedCount++
                        }
                    }
                }
                delay(100)
            }

            val pullOutcome = if (skipPull) {
                Result.Success(
                    PullSyncResult(
                        packsDownloaded = 0,
                        stickersDownloaded = 0,
                        itemsDeleted = 0,
                        downloadFailures = 0,
                    )
                )
            } else {
                _syncStage.value = SyncStage.PULLING_REMOTE
                pullProcessor.pullRemoteChanges()
            }

            val finalReport = when (pullOutcome) {
                is Result.Success -> {
                    val pull = pullOutcome.data
                    report.copy(
                        operationsProcessed = processedCount,
                        operationsSucceeded = successCount,
                        operationsFailed = failedCount,
                        packsSynced = pull.packsDownloaded,
                        stickersSynced = pull.stickersDownloaded,
                        remotePacksDownloaded = pull.packsDownloaded,
                        remoteStickersDownloaded = pull.stickersDownloaded,
                        remoteItemsDeleted = pull.itemsDeleted,
                        remoteDownloadFailures = pull.downloadFailures,
                        stage = SyncStage.IDLE,
                        result = if (failedCount > 0) SyncResult.Failed("$failedCount operations failed") else SyncResult.Success
                    ).also { _lastSyncReport.value = it }
                }
                is Result.Error -> report.copy(
                    operationsProcessed = processedCount,
                    operationsSucceeded = successCount,
                    operationsFailed = failedCount,
                    result = SyncResult.Failed(pullOutcome.error.detailText()),
                    stage = _syncStage.value,
                ).also { _lastSyncReport.value = it }
            }
            finalReport
        } catch (e: Exception) {
            val finalReport = report.copy(
                result = SyncResult.Failed(e.message ?: "Pull sync failed"),
                stage = _syncStage.value,
            )
            _lastSyncReport.value = finalReport
            finalReport
        } finally {
            _isSyncing.value = false
            _activeOperationFlow.value = null
            _syncStage.value = SyncStage.IDLE
        }
    }

    private suspend fun hasSyncCredentials(): Boolean = !authManager.getAccessToken().isNullOrBlank()

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

    override suspend fun sync(): SyncReport = processQueueInternal(skipPull = false)
}
