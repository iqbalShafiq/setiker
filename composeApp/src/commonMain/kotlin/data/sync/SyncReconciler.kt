package data.sync

import data.auth.AuthManager
import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.database.SyncOperationDao
import data.local.entity.StickerPackEntity
import data.sync.createPackSyncOperation
import data.sync.resolvePackSaveSyncTarget
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SYNC_STATE_SYNCED = "SYNCED"

@OptIn(ExperimentalUuidApi::class)
internal class SyncReconciler(
    private val operationDao: SyncOperationDao,
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val authManager: AuthManager,
) {
    /** Creates a pending CREATE_PACK operation for every pack that has never synced. */
    suspend fun reconcileLocalOnlyPacks(): Int {
        var createdOperations = 0
        packDao.getUnsynced().forEach { pack ->
            createdOperations += reconcilePackIfNeeded(pack)
        }
        return createdOperations
    }

    suspend fun reconcilePack(localPackId: String): Int {
        val pack = packDao.getById(localPackId) ?: return 0
        return reconcilePackIfNeeded(pack)
    }

    private suspend fun reconcilePackIfNeeded(pack: StickerPackEntity): Int {
        if (pack.syncState == SYNC_STATE_SYNCED) return 0
        if (!shouldSyncPackWithCloud(pack, authManager.getUser()?.id)) return 0

        val blockingOperations = operationDao.getBlockingOperations()
        val cloudId = pack.cloudId
        val hasLocalOperation = hasBlockingLocalOperation(blockingOperations, pack.identifier)
        val hasCloudOperation = cloudId != null && hasBlockingLocalOperation(blockingOperations, cloudId)
        if (hasLocalOperation || hasCloudOperation) return 0

        operationDao.insert(
            createPackSyncOperation(
                pack = pack,
                stickers = stickerDao.getByPackId(pack.identifier),
                id = Uuid.random().toString(),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                syncTarget = resolvePackSaveSyncTarget(
                    existing = pack,
                    localIdentifier = pack.identifier,
                    currentUserId = authManager.getUser()?.id,
                ),
            ).toPendingSyncEntity()
        )
        return 1
    }
}
