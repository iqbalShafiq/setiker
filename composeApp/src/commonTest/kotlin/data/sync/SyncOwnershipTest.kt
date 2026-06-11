package data.sync

import data.local.entity.StickerPackEntity
import data.remote.model.CloudStickerPack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncOwnershipTest {

    @Test
    fun isPackOwnedByUserTreatsNullOwnerAsLocalOnly() {
        assertTrue(isPackOwnedByUser(cloudOwnerId = null, currentUserId = "user-1"))
        assertTrue(isPackOwnedByUser(cloudOwnerId = "", currentUserId = "user-1"))
    }

    @Test
    fun isPackOwnedByUserMatchesCurrentUser() {
        assertTrue(isPackOwnedByUser(cloudOwnerId = "user-1", currentUserId = "user-1"))
        assertFalse(isPackOwnedByUser(cloudOwnerId = "user-2", currentUserId = "user-1"))
    }

    @Test
    fun shouldImportRemotePackForSyncOnlyOwnsPacks() {
        val remotePack = CloudStickerPack(
            id = "pack-1",
            ownerId = "user-2",
            name = "Other Pack",
            visibility = "PUBLIC",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )

        assertFalse(shouldImportRemotePackForSync(remotePack, currentUserId = "user-1"))
        assertTrue(shouldImportRemotePackForSync(remotePack.copy(ownerId = "user-1"), currentUserId = "user-1"))
    }

    @Test
    fun isForeignSyncedPackDetectsMistakenlySyncedRows() {
        val foreign = StickerPackEntity(
            identifier = "local-1",
            name = "Foreign",
            publisher = "Other",
            trayImageFile = "tray.webp",
            cloudId = "cloud-1",
            syncState = "SYNCED",
            cloudOwnerId = "user-2",
        )

        assertTrue(isForeignSyncedPack(foreign, currentUserId = "user-1"))
        assertFalse(isForeignSyncedPack(foreign.copy(cloudOwnerId = "user-1"), currentUserId = "user-1"))
        assertFalse(isForeignSyncedPack(foreign.copy(syncState = "LOCAL_ONLY"), currentUserId = "user-1"))
    }

    @Test
    fun shouldSyncPackWithCloudRequiresOwnership() {
        val owned = StickerPackEntity(
            identifier = "local-1",
            name = "Mine",
            publisher = "Me",
            trayImageFile = "tray.webp",
            cloudId = "cloud-1",
            cloudOwnerId = "user-1",
        )
        val imported = owned.copy(cloudOwnerId = "user-2")

        assertTrue(shouldSyncPackWithCloud(owned, currentUserId = "user-1"))
        assertFalse(shouldSyncPackWithCloud(imported, currentUserId = "user-1"))
    }
}
