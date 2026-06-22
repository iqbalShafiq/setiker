package data.repository

import data.auth.AuthManager
import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.database.compactSortOrders
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.storage.StickerFileStorage
import data.sync.SyncManager
import data.sync.createDeleteStickerSyncOperation
import data.sync.createPackSyncOperation
import data.sync.createPackVisibilitySyncOperation
import data.sync.normalizePackVisibilityForStorage
import data.sync.resolvePackSaveSyncTarget
import data.sync.shouldSyncPackWithCloud
import domain.error.AppErrorCode
import domain.error.AppException
import domain.model.Sticker
import domain.model.StickerDecoration
import domain.model.StickerPack
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncOperationType
import domain.model.SyncReport
import domain.model.SyncResult
import domain.model.decodeStickerDecorationsForCurrentSchema
import domain.model.normalizedForCurrentSchema
import domain.repository.StickerRepository
import domain.repository.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class StickerRepositoryImpl(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val fileStorage: StickerFileStorage,
    private val syncManager: SyncManager? = null,
    private val authManager: AuthManager? = null,
    private val cloudSyncedLocalDataCleaner: CloudSyncedLocalDataCleaner? = null,
) : StickerRepository {

    override suspend fun getAllPacks(): List<StickerPack> = withContext(Dispatchers.Default) {
        packDao.getAll().map { it.toDomainModel(emptyList()) }
            .map { pack ->
                val stickers = stickerDao.getByPackId(pack.identifier)
                    .map { it.toDomainModel() }
                pack.copy(stickers = stickers)
            }
    }

    override suspend fun getPack(identifier: String): StickerPack = withContext(Dispatchers.Default) {
        val entity = packDao.getById(identifier)
            ?: throw AppException(code = AppErrorCode.PackNotFound)
        val stickers = stickerDao.getByPackId(identifier)
            .map { it.toDomainModel() }
        entity.toDomainModel(stickers)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun savePack(pack: StickerPack, syncToCloud: Boolean) = withContext(Dispatchers.Default) {
        val identifier = pack.identifier.takeIf { it.isNotBlank() } ?: Uuid.random().toString()
        val existing = packDao.getById(identifier)
        val now = Clock.System.now().toEpochMilliseconds()
        val existingStickers = existing?.let { stickerDao.getByPackId(identifier) }.orEmpty()
        val entity = StickerPackEntity(
            identifier = identifier,
            name = pack.name,
            publisher = pack.publisher,
            trayImageFile = pack.trayImageFile,
            isAnimated = pack.isAnimated,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            cloudId = existing?.cloudId ?: pack.cloudId,
            syncState = when {
                syncToCloud && !existing?.cloudId.isNullOrBlank() -> "LOCAL_ONLY"
                else -> existing?.syncState ?: "LOCAL_ONLY"
            },
            lastSyncAt = existing?.lastSyncAt,
            visibility = pack.visibility,
            cloudOwnerId = existing?.cloudOwnerId ?: pack.cloudOwnerId,
        )
        packDao.upsert(entity)

        // Saving an existing pack is a full replacement of its current sticker
        // list. Without clearing old rows first, every edit (including only
        // changing the tray icon) appends duplicate stickers with new UUIDs.
        stickerDao.deleteByPackId(entity.identifier)
        pack.stickers.forEachIndexed { index, sticker ->
            val prior = existingStickers.getOrNull(index)
            val stickerEntity = StickerEntity(
                id = prior?.id ?: Uuid.random().toString(),
                packId = entity.identifier,
                imageFile = sticker.imageFile,
                sourceImageFile = sticker.sourceImageFile,
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                decorationsJson = Json.encodeToString(sticker.decorations),
                isAnimated = sticker.isAnimated,
                sourceVideoFile = sticker.sourceVideoFile,
                frameDecorationsJson = encodeFrameDecorations(sticker.frameDecorations),
                sortOrder = index,
                cloudId = prior?.cloudId,
                syncState = prior?.syncState ?: "LOCAL_ONLY",
                lastSyncAt = prior?.lastSyncAt,
                cloudUrl = prior?.cloudUrl,
            )
            stickerDao.insert(stickerEntity)
        }

        if (syncToCloud && authManager?.isAuthenticated() == true) {
            val persistedStickers = stickerDao.getByPackId(identifier)
            val syncTarget = resolvePackSaveSyncTarget(
                existing = entity,
                localIdentifier = identifier,
                currentUserId = authManager.getUser()?.id,
            )
            val syncOp = createPackSyncOperation(
                pack = entity,
                stickers = persistedStickers,
                id = Uuid.random().toString(),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                syncTarget = syncTarget,
            )
            syncManager?.enqueue(syncOp)
            syncManager?.pushPackOperations(identifier)
        }
    }

    override suspend fun deletePack(identifier: String) = withContext(Dispatchers.Default) {
        val pack = packDao.getById(identifier) ?: return@withContext
        val cloudPackId = pack.cloudId

        if (
            !cloudPackId.isNullOrBlank() &&
            authManager?.isAuthenticated() == true &&
            shouldSyncPackWithCloud(pack, authManager.getUser()?.id)
        ) {
            val syncOp = SyncOperation(
                id = Uuid.random().toString(),
                type = SyncOperationType.DELETE_PACK,
                targetId = cloudPackId,
                payload = "{}",
                status = SyncOperationStatus.PENDING,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            syncManager?.enqueue(syncOp)
            syncManager?.pushPackOperations(identifier, cloudPackId)
        }

        val stickers = stickerDao.getByPackId(identifier)
        stickers.forEach { sticker ->
            listOfNotNull(sticker.imageFile, sticker.sourceImageFile).distinct().forEach {
                fileStorage.deleteImage(it)
            }
        }
        fileStorage.deleteImage(pack.trayImageFile)

        stickerDao.deleteByPackId(identifier)
        packDao.delete(pack)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addStickerToPack(packId: String, sticker: Sticker) {
        withContext(Dispatchers.Default) {
            val count = stickerDao.getCountByPackId(packId)
            val entity = StickerEntity(
                id = Uuid.random().toString(),
                packId = packId,
                imageFile = sticker.imageFile,
                sourceImageFile = sticker.sourceImageFile,
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                decorationsJson = Json.encodeToString(sticker.decorations),
                isAnimated = sticker.isAnimated,
                sourceVideoFile = sticker.sourceVideoFile,
                frameDecorationsJson = encodeFrameDecorations(sticker.frameDecorations),
                sortOrder = count
            )
            stickerDao.insert(entity)

            packDao.getById(packId)?.let { pack ->
                markPackDirty(pack)
            }
        }
        syncManager?.pushPackOperations(packId)
    }

    override suspend fun updateStickerInPack(packId: String, index: Int, sticker: Sticker) {
        withContext(Dispatchers.Default) {
            val stickers = stickerDao.getByPackId(packId)
            val existing = stickers.getOrNull(index)
                ?: throw AppException(code = AppErrorCode.StickerNotFound)

            val updatedEntity = existing.copy(
                imageFile = sticker.imageFile,
                sourceImageFile = sticker.sourceImageFile,
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                decorationsJson = Json.encodeToString(sticker.decorations),
                isAnimated = sticker.isAnimated,
                sourceVideoFile = sticker.sourceVideoFile,
                frameDecorationsJson = encodeFrameDecorations(sticker.frameDecorations)
            )
            stickerDao.insert(updatedEntity)

            packDao.getById(packId)?.let { pack ->
                markPackDirty(pack)
            }
        }
        syncManager?.pushPackOperations(packId)
    }

    override suspend fun removeStickerFromPack(packId: String, index: Int) = withContext(Dispatchers.Default) {
        val stickers = stickerDao.getByPackId(packId)
        val stickerToDelete = stickers.getOrNull(index) ?: return@withContext
        val pack = packDao.getById(packId) ?: return@withContext
        val now = Clock.System.now().toEpochMilliseconds()

        listOfNotNull(stickerToDelete.imageFile, stickerToDelete.sourceImageFile).distinct().forEach {
            fileStorage.deleteImage(it)
        }
        stickerDao.deleteById(stickerToDelete.id)
        stickerDao.compactSortOrders(packId)
        packDao.update(pack.copy(updatedAt = now))

        if (authManager?.isAuthenticated() != true) {
            return@withContext
        }

        val cloudPackId = pack.cloudId
        if (cloudPackId.isNullOrBlank() || !shouldSyncPackWithCloud(pack, authManager.getUser()?.id)) {
            return@withContext
        }

        val cloudStickerId = stickerToDelete.cloudId
        if (!cloudStickerId.isNullOrBlank()) {
            syncManager?.enqueue(
                createDeleteStickerSyncOperation(
                    cloudPackId = cloudPackId,
                    cloudStickerId = cloudStickerId,
                    id = Uuid.random().toString(),
                    createdAt = now,
                )
            )
        } else {
            markPackDirty(pack)
        }
        syncManager?.pushPackOperations(packId, cloudPackId)
    }

    private suspend fun markPackDirty(pack: StickerPackEntity) {
        packDao.update(
            pack.copy(
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                syncState = "LOCAL_ONLY",
                lastSyncAt = null,
            )
        )
    }

    private fun StickerPackEntity.toDomainModel(stickers: List<Sticker>) = StickerPack(
        identifier = identifier,
        name = name,
        publisher = publisher,
        trayImageFile = trayImageFile,
        stickers = stickers,
        isAnimated = isAnimated,
        cloudId = cloudId,
        syncState = syncState,
        lastSyncAt = lastSyncAt,
        visibility = visibility,
        cloudOwnerId = cloudOwnerId
    )

    private fun StickerEntity.toDomainModel() = Sticker(
        imageFile = imageFile,
        sourceImageFile = sourceImageFile,
        emojis = Json.decodeFromString(emojis),
        accessibilityText = accessibilityText,
        decorations = parseDecorations(decorationsJson),
        isAnimated = isAnimated,
        sourceVideoFile = sourceVideoFile,
        frameDecorations = parseFrameDecorations(frameDecorationsJson)
    )

    private fun parseDecorations(raw: String?): List<StickerDecoration> {
        return decodeStickerDecorationsForCurrentSchema(raw)
    }

    private fun encodeFrameDecorations(map: Map<Int, List<StickerDecoration>>): String? {
        if (map.isEmpty()) return null
        val stringKeyed: Map<String, List<StickerDecoration>> = map.mapKeys { it.key.toString() }
        return Json.encodeToString(stringKeyed)
    }

    private fun parseFrameDecorations(raw: String?): Map<Int, List<StickerDecoration>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val stringKeyed: Map<String, List<StickerDecoration>> = Json.decodeFromString(raw)
            stringKeyed.mapNotNull { (k, v) ->
                k.toIntOrNull()?.let { it to v.map { decoration -> decoration.normalizedForCurrentSchema() } }
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override suspend fun updatePackVisibility(packId: String, visibility: String) = withContext(Dispatchers.Default) {
        val existing = packDao.getById(packId) ?: throw AppException(code = AppErrorCode.PackNotFound)
        val normalizedVisibility = normalizePackVisibilityForStorage(visibility)
        val now = Clock.System.now().toEpochMilliseconds()
        packDao.upsert(existing.copy(visibility = normalizedVisibility, updatedAt = now))

        if (authManager?.isAuthenticated() != true) {
            return@withContext
        }

        val cloudId = existing.cloudId
        if (!cloudId.isNullOrBlank() && shouldSyncPackWithCloud(existing, authManager.getUser()?.id)) {
            if (existing.syncState == "SYNCED") {
                syncManager?.cancelPendingContentUploadOps(packId, cloudId)
            }
            syncManager?.enqueue(
                createPackVisibilitySyncOperation(
                    cloudPackId = cloudId,
                    visibility = normalizedVisibility,
                    id = Uuid.random().toString(),
                    createdAt = now,
                )
            )
            syncManager?.pushPackVisibilityOperations(packId, cloudId)
        } else {
            // Pack belum ada di cloud: upload sekali dengan visibility target.
            markPackDirty(existing.copy(visibility = normalizedVisibility))
            syncManager?.pushPackOperations(packId)
        }
    }

    override suspend fun clearCloudSyncedDataOnLogout() {
        withContext(Dispatchers.Default) {
            cloudSyncedLocalDataCleaner?.clearOnLogout()
        }
    }

    override suspend fun syncAll(): SyncReport {
        return syncManager?.sync() ?: SyncReport(result = SyncResult.SkippedNotAuthenticated)
    }

    override suspend fun syncPack(packId: String): SyncReport {
        return syncManager?.pushPackOperations(packId) ?: SyncReport(result = SyncResult.SkippedNotAuthenticated)
    }

    override fun observeSyncStatus(): Flow<SyncStatus> {
        return syncManager?.operationsFlow?.map { operations ->
            when {
                operations.any { it.status == SyncOperationStatus.IN_PROGRESS } -> SyncStatus.Syncing
                operations.any { it.status == SyncOperationStatus.FAILED } -> SyncStatus.Failed
                operations.any { it.status == SyncOperationStatus.PENDING } -> SyncStatus.Pending
                else -> SyncStatus.Idle
            }
        } ?: flowOf(SyncStatus.Idle)
    }

    override suspend fun getPendingSyncCount(): Int {
        return syncManager?.operationsFlow?.first()?.count {
            it.status == SyncOperationStatus.PENDING
        } ?: 0
    }

    override suspend fun duplicatePack(identifier: String): String = withContext(Dispatchers.Default) {
        val source = getPack(identifier)
        val newId = Uuid.random().toString()
        val trayPath = fileStorage.saveTrayImage(
            source.trayImageFile,
            "tray_${newId}_${Clock.System.now().toEpochMilliseconds()}.webp"
        )
        val copiedStickers = source.stickers.mapIndexed { index, sticker ->
            val imagePath = fileStorage.saveStickerImage(
                sticker.imageFile,
                "sticker_${newId}_$index.webp"
            )
            sticker.copy(
                imageFile = imagePath,
                sourceImageFile = sticker.sourceImageFile?.let { sourcePath ->
                    fileStorage.saveImage(sourcePath, "src_${newId}_$index.webp")
                }
            )
        }
        val duplicate = source.copy(
            identifier = newId,
            name = "${source.name} (copy)",
            stickers = copiedStickers,
            trayImageFile = trayPath
        )
        savePack(duplicate)
        newId
    }
}
