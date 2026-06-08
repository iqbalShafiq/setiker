package data.sync

import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.remote.model.CreateStickerPackRequest
import data.remote.model.DeleteStickerSyncPayload
import data.remote.model.StickerPackStickerInput
import data.remote.model.UpdateStickerPackRequest
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncOperationType
import kotlinx.serialization.json.Json

data class PackSaveSyncTarget(
    val type: SyncOperationType,
    val targetId: String,
)

fun resolvePackSaveSyncTarget(
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

fun createPackSyncOperation(
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
            visibility = normalizePackVisibilityForApi(pack.visibility),
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

fun createDeleteStickerSyncOperation(
    cloudPackId: String,
    cloudStickerId: String,
    id: String,
    createdAt: Long,
): SyncOperation = SyncOperation(
    id = id,
    type = SyncOperationType.DELETE_STICKER,
    targetId = cloudStickerId,
    payload = Json.encodeToString(
        DeleteStickerSyncPayload(
            stickerPackId = cloudPackId,
            stickerId = cloudStickerId,
        )
    ),
    status = SyncOperationStatus.PENDING,
    createdAt = createdAt,
)

fun operationIsPackVisibilityUpdate(operation: SyncOperation): Boolean =
    operation.type == SyncOperationType.UPDATE_PACK_VISIBILITY

/**
 * Only replace local sticker files when we successfully imported remote bytes.
 * Never wipe local stickers just because the remote payload had an empty list.
 */
fun shouldReplaceLocalStickersFromRemote(importedStickerCount: Int): Boolean =
    importedStickerCount > 0

/**
 * Packs that already exist locally with sticker content are the source of truth
 * for assets on-device. Background pull may only merge metadata (e.g. visibility).
 */
fun shouldPreserveLocalStickersOnPull(
    hasExistingPack: Boolean,
    localStickerCount: Int,
): Boolean = hasExistingPack && localStickerCount > 0

/**
 * Only download remote sticker bytes into a pack that already exists locally when
 * the device has no local sticker rows yet (new device / recovery).
 */
fun shouldImportStickersIntoExistingPack(
    localStickerCount: Int,
    remoteStickerCount: Int,
): Boolean = localStickerCount == 0 && remoteStickerCount > 0

/**
 * Cloud-linked packs on this device manage sticker deletions via push
 * (DELETE_STICKER). Ignore remote sticker deletion deltas during pull.
 */
fun shouldIgnoreRemoteStickerDeletion(parentPack: StickerPackEntity?): Boolean =
    parentPack != null && !parentPack.cloudId.isNullOrBlank()

fun operationBelongsToPack(
    operation: SyncOperation,
    localPackId: String,
    cloudPackId: String?,
): Boolean {
    return when (operation.type) {
        SyncOperationType.CREATE_PACK -> operation.targetId == localPackId
        SyncOperationType.UPDATE_PACK,
        SyncOperationType.UPDATE_PACK_VISIBILITY,
        SyncOperationType.DELETE_PACK -> !cloudPackId.isNullOrBlank() && operation.targetId == cloudPackId
        SyncOperationType.DELETE_STICKER -> {
            if (cloudPackId.isNullOrBlank()) return false
            val payload = Json.decodeFromString<DeleteStickerSyncPayload>(operation.payload)
            payload.stickerPackId == cloudPackId
        }
        else -> false
    }
}

fun createPackVisibilitySyncOperation(
    cloudPackId: String,
    visibility: String,
    id: String,
    createdAt: Long,
): SyncOperation = SyncOperation(
    id = id,
    type = SyncOperationType.UPDATE_PACK_VISIBILITY,
    targetId = cloudPackId,
    payload = Json.encodeToString(
        UpdateStickerPackRequest(
            visibility = normalizePackVisibilityForApi(visibility),
        )
    ),
    status = SyncOperationStatus.PENDING,
    createdAt = createdAt,
)
