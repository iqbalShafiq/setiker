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
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
import domain.model.SyncOperation
import domain.model.SyncOperationType
import kotlin.time.Clock

private const val SYNC_STATE_SYNCED = "SYNCED"

/**
 * Push-side operation execution (spec P3b, D4/D6). Every cloud call returns a typed
 * [Result]; the first [Result.Error] short-circuits out of [execute]. The queue owner in
 * `SyncManagerImpl` reads `error.retryable` to apply the 5-retry cap. Coroutine cancellation
 * propagates untouched instead of being recorded as an operation failure.
 */
internal class SyncOperationExecutor(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val syncCursorStore: SyncCursorStore,
) {
    private val json = ApiJson.codec

    suspend fun execute(operation: SyncOperation): EmptyResult<DataError.Network> {
        try {
            when (operation.type) {
                SyncOperationType.CREATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    val upload = when (val outcome = cloudRepo.uploadPack(request)) {
                        is Result.Success -> outcome.data
                        is Result.Error -> return Result.Error(outcome.error)
                    }
                    val cloudPackId = upload.stickerPackId
                        ?: return Result.Error(
                            DataError.Network.ApiRejected(
                                appCode = null,
                                message = "Upload did not return sticker pack id",
                            )
                        )
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
                    Result.Success(Unit)
                }
                SyncOperationType.UPDATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    if (request.stickers.isEmpty()) {
                        return Result.Success(Unit)
                    }
                    val localPack = packDao.getByCloudId(operation.targetId)
                        ?: return Result.Error(
                            DataError.Network.ApiRejected(
                                appCode = null,
                                message = "Local pack not found for cloud id ${operation.targetId}",
                            )
                        )
                    if (localPack.syncState == SYNC_STATE_SYNCED) {
                        return Result.Success(Unit)
                    }
                    val now = Clock.System.now().toEpochMilliseconds()
                    val upload = when (val outcome = cloudRepo.uploadPack(request, stickerPackId = operation.targetId)) {
                        is Result.Success -> outcome.data
                        is Result.Error -> return Result.Error(outcome.error)
                    }
                    packDao.updateSyncStatus(localPack.identifier, SYNC_STATE_SYNCED, now)
                    updateLocalStickerCloudInfo(localPack.identifier, upload.stickers)
                    syncCursorStore.setLastPullSyncAt(now)
                    Result.Success(Unit)
                }
                SyncOperationType.UPDATE_PACK_VISIBILITY -> {
                    val request = json.decodeFromString<UpdateStickerPackRequest>(operation.payload)
                    val updatedPack = when (val outcome = cloudRepo.updatePack(operation.targetId, request)) {
                        is Result.Success -> outcome.data
                        is Result.Error -> return Result.Error(outcome.error)
                    }
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
                    Result.Success(Unit)
                }
                SyncOperationType.DELETE_PACK -> {
                    when (val outcome = cloudRepo.deletePackViaUpload(operation.targetId)) {
                        is Result.Success -> outcome.data
                        is Result.Error -> return Result.Error(outcome.error)
                    }
                    Result.Success(Unit)
                }
                SyncOperationType.DELETE_STICKER -> {
                    val payload = json.decodeFromString<DeleteStickerSyncPayload>(operation.payload)
                    when (val outcome = cloudRepo.deleteStickerFromPack(payload.stickerPackId, payload.stickerId)) {
                        is Result.Success -> outcome.data
                        is Result.Error -> return Result.Error(outcome.error)
                    }
                    Result.Success(Unit)
                }
                SyncOperationType.REORDER_STICKERS -> {
                    val payload = json.decodeFromString<data.remote.model.ReorderStickersSyncPayload>(operation.payload)
                    when (val outcome = cloudRepo.reorderStickers(payload.stickerPackId, payload.stickerOrders)) {
                        is Result.Success -> outcome.data
                        is Result.Error -> return Result.Error(outcome.error)
                    }
                    Result.Success(Unit)
                }
                else -> return Result.Error(
                    DataError.Network.ApiRejected(
                        appCode = null,
                        message = "Operation type ${operation.type} not implemented",
                    )
                )
            }
            return Result.Success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Cancelling the queue worker must not be recorded as an operation failure.
            throw e
        } catch (e: Exception) {
            return Result.Error(e.toDataError())
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
