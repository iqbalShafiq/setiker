package data.repository

import data.auth.AuthManager
import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.remote.model.CreateStickerPackRequest
import data.remote.model.StickerPackStickerInput
import data.storage.StickerFileStorage
import data.sync.SyncManager
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
    private val authManager: AuthManager? = null
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
    override suspend fun savePack(pack: StickerPack) = withContext(Dispatchers.Default) {
        val identifier = pack.identifier.takeIf { it.isNotBlank() } ?: Uuid.random().toString()
        val existing = packDao.getById(identifier)
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = StickerPackEntity(
            identifier = identifier,
            name = pack.name,
            publisher = pack.publisher,
            trayImageFile = pack.trayImageFile,
            isAnimated = pack.isAnimated,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            cloudId = existing?.cloudId,
            syncState = existing?.syncState ?: "LOCAL_ONLY",
            lastSyncAt = existing?.lastSyncAt,
            visibility = pack.visibility,
            cloudOwnerId = existing?.cloudOwnerId,
        )
        packDao.insert(entity)

        // Enqueue cloud sync if authenticated
        if (authManager?.isAuthenticated() == true) {
            val syncTarget = resolvePackSaveSyncTarget(existing, identifier)
            val syncOp = createPackSyncOperation(
                pack = entity,
                stickers = pack.stickers.mapIndexed { index, sticker ->
                    StickerEntity(
                        id = Uuid.random().toString(),
                        packId = identifier,
                        imageFile = sticker.imageFile,
                        sourceImageFile = sticker.sourceImageFile,
                        emojis = Json.encodeToString(sticker.emojis),
                        accessibilityText = sticker.accessibilityText,
                        decorationsJson = Json.encodeToString(sticker.decorations),
                        isAnimated = sticker.isAnimated,
                        sourceVideoFile = sticker.sourceVideoFile,
                        frameDecorationsJson = encodeFrameDecorations(sticker.frameDecorations),
                        sortOrder = index,
                    )
                },
                id = Uuid.random().toString(),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                syncTarget = syncTarget,
            )
            syncManager?.enqueue(syncOp)
        }

        // Saving an existing pack is a full replacement of its current sticker
        // list. Without clearing old rows first, every edit (including only
        // changing the tray icon) appends duplicate stickers with new UUIDs.
        stickerDao.deleteByPackId(entity.identifier)
        pack.stickers.forEachIndexed { index, sticker ->
            val stickerEntity = StickerEntity(
                id = Uuid.random().toString(),
                packId = entity.identifier,
                imageFile = sticker.imageFile,
                sourceImageFile = sticker.sourceImageFile,
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                decorationsJson = Json.encodeToString(sticker.decorations),
                isAnimated = sticker.isAnimated,
                sourceVideoFile = sticker.sourceVideoFile,
                frameDecorationsJson = encodeFrameDecorations(sticker.frameDecorations),
                sortOrder = index
            )
            stickerDao.insert(stickerEntity)
        }
    }

    override suspend fun deletePack(identifier: String) = withContext(Dispatchers.Default) {
        val pack = packDao.getById(identifier) ?: return@withContext

        val stickers = stickerDao.getByPackId(identifier)
        stickers.forEach { sticker ->
            listOfNotNull(sticker.imageFile, sticker.sourceImageFile).distinct().forEach {
                fileStorage.deleteImage(it)
            }
        }
        fileStorage.deleteImage(pack.trayImageFile)

        stickerDao.deleteByPackId(identifier)
        packDao.delete(pack)

        if (pack.cloudId != null && authManager?.isAuthenticated() == true) {
            val syncOp = SyncOperation(
                id = Uuid.random().toString(),
                type = SyncOperationType.DELETE_PACK,
                targetId = pack.cloudId,
                payload = "{}",
                status = SyncOperationStatus.PENDING,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            syncManager?.enqueue(syncOp)
        }
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
        syncManager?.sync()
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
        syncManager?.sync()
    }

    override suspend fun removeStickerFromPack(packId: String, index: Int) = withContext(Dispatchers.Default) {
        val stickers = stickerDao.getByPackId(packId)
        val stickerToDelete = stickers.getOrNull(index) ?: return@withContext

        listOfNotNull(stickerToDelete.imageFile, stickerToDelete.sourceImageFile).distinct().forEach {
            fileStorage.deleteImage(it)
        }
        stickerDao.deleteByPackAndIndex(packId, index)
        packDao.getById(packId)?.let { pack ->
            markPackDirty(pack)
        }
        syncManager?.sync()
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
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString<List<StickerDecoration>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
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
            stringKeyed.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override suspend fun syncAll(): SyncReport {
        return syncManager?.sync() ?: SyncReport(result = SyncResult.SkippedNotAuthenticated)
    }

    override suspend fun syncPack(packId: String): SyncReport {
        return syncManager?.sync() ?: SyncReport(result = SyncResult.SkippedNotAuthenticated)
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
}

internal data class PackSaveSyncTarget(
    val type: SyncOperationType,
    val targetId: String,
)

internal fun resolvePackSaveSyncTarget(
    existing: StickerPackEntity?,
    localIdentifier: String,
): PackSaveSyncTarget {
    val cloudId = existing?.cloudId
    return if (cloudId.isNullOrBlank()) {
        PackSaveSyncTarget(SyncOperationType.CREATE_PACK, localIdentifier)
    } else {
        PackSaveSyncTarget(SyncOperationType.UPDATE_PACK, cloudId)
    }
}

internal fun createPackSyncOperation(
    pack: StickerPackEntity,
    stickers: List<StickerEntity>,
    id: String,
    createdAt: Long,
    syncTarget: PackSaveSyncTarget = resolvePackSaveSyncTarget(pack, pack.identifier),
): SyncOperation = SyncOperation(
    id = id,
    type = syncTarget.type,
    targetId = syncTarget.targetId,
    payload = Json.encodeToString(
        CreateStickerPackRequest(
            name = pack.name,
            description = null,
            visibility = pack.visibility,
            stickers = stickers.sortedBy { it.sortOrder }.mapIndexed { index, sticker ->
                StickerPackStickerInput(
                    name = sticker.accessibilityText ?: "sticker_$index",
                    filename = sticker.imageFile.substringAfterLast("/"),
                    url = sticker.imageFile,
                    order = index,
                )
            },
        )
    ),
    status = SyncOperationStatus.PENDING,
    createdAt = createdAt,
)
