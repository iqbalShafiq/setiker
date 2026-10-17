package data.sync

import data.auth.AuthManager
import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.database.SyncOperationDao
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.remote.CloudStickerRepository
import data.remote.model.CloudStickerPack
import data.storage.StickerFileStorage
import data.sync.normalizePackVisibilityForStorage
import data.sync.shouldIgnoreRemoteStickerDeletion
import data.sync.shouldImportStickersIntoExistingPack
import data.sync.shouldPreserveLocalStickersOnPull
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SYNC_STATE_SYNCED = "SYNCED"

/**
 * Pull-side pipeline (spec P3b, D4): fetch the remote delta, import/merge packs and stickers
 * (with file hydration), honor deletion conflicts, and advance the pull cursor.
 * Moved verbatim from `SyncManagerImpl`.
 */
@OptIn(ExperimentalUuidApi::class)
internal class RemotePullProcessor(
    private val operationDao: SyncOperationDao,
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val fileStorage: StickerFileStorage,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val syncCursorStore: SyncCursorStore,
) {
    suspend fun pullRemoteChanges(): PullSyncResult {
        val syncData = cloudRepo.sync(syncCursorStore.getLastPullSyncAt())
        val currentUserId = authManager.getUser()?.id
        val blockingOperations = operationDao.getBlockingOperations()
        var packsDownloaded = 0
        var stickersDownloaded = 0
        var itemsDeleted = 0
        var downloadFailures = 0

        purgeForeignSyncedPacks(currentUserId)

        val packDelta = syncData.stickerPacks
        val remotePacks = (packDelta?.created.orEmpty() + packDelta?.updated.orEmpty())
            .distinctBy { it.id }
            .filter { shouldImportRemotePackForSync(it, currentUserId) }
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
            val sticker = stickerDao.getByCloudId(deletedSticker.id) ?: continue
            val parentPack = packDao.getById(sticker.packId)
            if (shouldIgnoreRemoteStickerDeletion(parentPack)) continue
            deleteStickerFiles(sticker)
            stickerDao.deleteByCloudId(deletedSticker.id)
            itemsDeleted += 1
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

    private suspend fun purgeForeignSyncedPacks(currentUserId: String?) {
        if (currentUserId.isNullOrBlank()) return
        packDao.getAll()
            .filter { isForeignSyncedPack(it, currentUserId) }
            .forEach { pack ->
                deleteLocalPackFiles(pack.identifier)
                packDao.delete(pack)
            }
    }

    private suspend fun importRemotePack(remotePack: CloudStickerPack): RemotePackImportResult {
        val existing = packDao.getByCloudId(remotePack.id)
        if (existing != null) {
            val localStickerCount = stickerDao.getByPackId(existing.identifier).size
            mergeRemotePackMetadata(existing, remotePack)
            if (shouldPreserveLocalStickersOnPull(hasExistingPack = true, localStickerCount = localStickerCount)) {
                return RemotePackImportResult(
                    stickersImported = 0,
                    downloadFailures = 0,
                )
            }
            val remoteStickerCount = remotePack.stickers.size
            if (!shouldImportStickersIntoExistingPack(localStickerCount, remoteStickerCount)) {
                return RemotePackImportResult(
                    stickersImported = 0,
                    downloadFailures = 0,
                )
            }
            val hydrated = downloadRemoteStickers(existing.identifier, remotePack)
            hydrated.stickers.forEach { stickerDao.insert(it) }
            return RemotePackImportResult(
                stickersImported = hydrated.stickers.size,
                downloadFailures = hydrated.downloadFailures,
            )
        }

        val localIdentifier = Uuid.random().toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val publisher = remotePack.owner?.displayName?.takeIf { it.isNotBlank() }
            ?: remotePack.owner?.username?.takeIf { it.isNotBlank() }
            ?: "Setiker"

        val mergedPack = StickerPackEntity(
            identifier = localIdentifier,
            name = remotePack.name,
            publisher = publisher,
            trayImageFile = "",
            isAnimated = false,
            createdAt = now,
            updatedAt = now,
            cloudId = remotePack.id,
            syncState = SYNC_STATE_SYNCED,
            lastSyncAt = now,
            visibility = normalizePackVisibilityForStorage(remotePack.visibility),
            cloudOwnerId = remotePack.ownerId,
        )
        packDao.insert(mergedPack)

        val imported = downloadRemoteStickers(localIdentifier, remotePack)
        imported.stickers.forEach { stickerDao.insert(it) }
        if (imported.stickers.isNotEmpty()) {
            packDao.upsert(mergedPack.copy(trayImageFile = imported.stickers.first().imageFile))
        }

        return RemotePackImportResult(
            stickersImported = imported.stickers.size,
            downloadFailures = imported.downloadFailures,
        )
    }

    private suspend fun downloadRemoteStickers(
        localPackId: String,
        remotePack: CloudStickerPack,
    ): RemoteStickerImportResult {
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
                    packId = localPackId,
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

        return RemoteStickerImportResult(
            stickers = importedStickers,
            downloadFailures = downloadFailures,
        )
    }

    private suspend fun mergeRemotePackMetadata(
        existing: StickerPackEntity,
        remotePack: CloudStickerPack,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val syncState = if (existing.syncState == "LOCAL_ONLY") {
            existing.syncState
        } else {
            SYNC_STATE_SYNCED
        }
        packDao.upsert(
            existing.copy(
                visibility = normalizePackVisibilityForStorage(remotePack.visibility),
                syncState = syncState,
                lastSyncAt = now,
                updatedAt = now,
                cloudOwnerId = remotePack.ownerId,
            )
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
}
