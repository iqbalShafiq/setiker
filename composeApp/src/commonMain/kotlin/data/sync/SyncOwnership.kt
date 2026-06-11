package data.sync

import data.local.entity.StickerPackEntity
import data.remote.model.CloudStickerPack

/**
 * Returns true when [cloudOwnerId] is absent (local-only pack) or matches [currentUserId].
 */
fun isPackOwnedByUser(cloudOwnerId: String?, currentUserId: String?): Boolean {
    if (currentUserId.isNullOrBlank()) return false
    return cloudOwnerId.isNullOrBlank() || cloudOwnerId == currentUserId
}

/**
 * Pull sync should only hydrate packs the authenticated user owns.
 * Shared and public catalog packs use dedicated flows (share accept, import, explore).
 */
fun shouldImportRemotePackForSync(remoteOwnerId: String, currentUserId: String?): Boolean {
    if (currentUserId.isNullOrBlank()) return false
    return remoteOwnerId == currentUserId
}

fun shouldImportRemotePackForSync(remotePack: CloudStickerPack, currentUserId: String?): Boolean =
    shouldImportRemotePackForSync(remotePack.ownerId, currentUserId)

/**
 * Only cloud-linked packs owned by the signed-in user may be pushed, reconciled, or deleted remotely.
 */
fun shouldSyncPackWithCloud(pack: StickerPackEntity, currentUserId: String?): Boolean =
    isPackOwnedByUser(pack.cloudOwnerId, currentUserId)

/**
 * Locally synced rows that reference another user's cloud account were imported by mistake
 * (e.g. before sync scope was fixed server-side) and should be purged on the next pull.
 */
fun isForeignSyncedPack(pack: StickerPackEntity, currentUserId: String?): Boolean {
    if (currentUserId.isNullOrBlank()) return false
    if (pack.syncState != "SYNCED") return false
    val ownerId = pack.cloudOwnerId ?: return false
    return ownerId != currentUserId
}
