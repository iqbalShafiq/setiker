package data.sync

import data.auth.AuthManager
import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.remote.CloudStickerRepository
import data.remote.json.ApiJson
import data.remote.model.CreateStickerPackRequest
import data.remote.model.DeleteStickerSyncPayload
import data.remote.model.UpdateStickerPackRequest
import data.remote.toDataError
import data.sync.normalizePackVisibilityForStorage
import domain.error.detailText
import domain.model.SyncOperation
import domain.model.SyncOperationType
import kotlinx.serialization.json.Json
import kotlin.time.Clock

private const val SYNC_STATE_SYNCED = "SYNCED"

/**
 * Outcome of executing one pending sync operation against the cloud.
 * `RetryableError` is classified via the typed transport classifier (`toDataError`); the
 * executor's caller applies the 5-retry cap.
 */
sealed class OperationResult {
    data object Success : OperationResult()
    data class RetryableError(val error: String) : OperationResult()
    data class PermanentError(val error: String) : OperationResult()
}

/**
 * Push-side operation execution (spec P3b, D4). Moved verbatim from `SyncManagerImpl` — the
 * only behavioral change is the typed retry classification replacing message string-matching.
 */
internal class SyncOperationExecutor(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val syncCursorStore: SyncCursorStore,
) {
    private val json = ApiJson.codec

    suspend fun execute(operation: SyncOperation): OperationResult {
        return try {
            when (operation.type) {
                SyncOperationType.CREATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    val upload = cloudRepo.uploadPack(request)
                    val cloudPackId = upload.stickerPackId
                        ?: return OperationResult.PermanentError("Upload did not return sticker pack id")
                    val now = Clock.System.now().toEpochMilliseconds()
                    packDao.updateCloudId(operation.targetId, cloudPackId)
                    packDao.updateSyncStatus(operation.targetId, SYNC_STATE_SYNCED, now)
                    authManager.getUser()?.id?.let { ownerId ->
                        packDao.getById(operation.targetId)?.let { pack ->
                            packDao.upsert(pack.copy(cloudOwnerId = ownerId))
                        }
                    }
                    updateLocalStickerCloudInfo(operation.targetId, upload.stickers)
                    syncCursorStore.setLastPullSyncAt(now)
                    OperationResult.Success
                }
                SyncOperationType.UPDATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    if (request.stickers.isEmpty()) {
                        return OperationResult.Success
                    }
                    val localPack = packDao.getByCloudId(operation.targetId)
                        ?: return OperationResult.PermanentError("Local pack not found for cloud id ${operation.targetId}")
                    if (localPack.syncState == SYNC_STATE_SYNCED) {
                        return OperationResult.Success
                    }
                    val now = Clock.System.now().toEpochMilliseconds()
                    val upload = cloudRepo.uploadPack(request, stickerPackId = operation.targetId)
                    packDao.updateSyncStatus(localPack.identifier, SYNC_STATE_SYNCED, now)
                    updateLocalStickerCloudInfo(localPack.identifier, upload.stickers)
                    syncCursorStore.setLastPullSyncAt(now)
                    OperationResult.Success
                }
                SyncOperationType.UPDATE_PACK_VISIBILITY -> {
                    val request = json.decodeFromString<UpdateStickerPackRequest>(operation.payload)
                    val updatedPack = cloudRepo.updatePack(operation.targetId, request)
                    val now = Clock.System.now().toEpochMilliseconds()
                    packDao.getByCloudId(operation.targetId)?.let { pack ->
                        packDao.upsert(
                            pack.copy(
                                visibility = normalizePackVisibilityForStorage(updatedPack.visibility),
                                syncState = SYNC_STATE_SYNCED,
                                lastSyncAt = now,
                                updatedAt = now,
                            )
                        )
                    }
                    // Visibility updates also bump remote updatedAt; advance cursor so a
                    // background pull does not immediately re-import and wipe local stickers.
                    syncCursorStore.setLastPullSyncAt(now)
                    OperationResult.Success
                }
                SyncOperationType.DELETE_PACK -> {
                    cloudRepo.deletePackViaUpload(operation.targetId)
                    OperationResult.Success
                }
                SyncOperationType.DELETE_STICKER -> {
                    val payload = json.decodeFromString<DeleteStickerSyncPayload>(operation.payload)
                    cloudRepo.deleteStickerFromPack(payload.stickerPackId, payload.stickerId)
                    OperationResult.Success
                }
                SyncOperationType.REORDER_STICKERS -> {
                    val payload = json.decodeFromString<data.remote.model.ReorderStickersSyncPayload>(operation.payload)
                    cloudRepo.reorderStickers(payload.stickerPackId, payload.stickerOrders)
                    OperationResult.Success
                }
                else -> OperationResult.PermanentError("Operation type ${operation.type} not implemented")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Cancelling the queue worker must not be recorded as an operation failure.
            throw e
        } catch (e: Exception) {
            val classified = e.toDataError()
            when {
                classified.retryable -> OperationResult.RetryableError(classified.detailText())
                else -> OperationResult.PermanentError(classified.detailText())
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
}
