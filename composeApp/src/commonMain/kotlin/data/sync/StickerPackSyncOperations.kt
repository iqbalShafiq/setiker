package data.sync

import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.remote.model.CreateStickerPackRequest
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
