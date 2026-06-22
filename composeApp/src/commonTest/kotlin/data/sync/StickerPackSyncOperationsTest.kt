package data.sync

import data.local.entity.StickerPackEntity
import data.remote.model.DeleteStickerSyncPayload
import data.remote.model.UpdateStickerPackRequest
import domain.model.SyncOperationType
import kotlinx.serialization.json.Json
import data.sync.operationBelongsToPack
import kotlin.test.Test
import kotlin.test.assertEquals

class StickerPackSyncOperationsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun createPackVisibilitySyncOperationUsesPutPayload() {
        val operation = createPackVisibilitySyncOperation(
            cloudPackId = "cloud-pack-1",
            visibility = "PUBLIC",
            id = "op-visibility",
            createdAt = 42L,
        )

        assertEquals(SyncOperationType.UPDATE_PACK_VISIBILITY, operation.type)
        assertEquals("cloud-pack-1", operation.targetId)
        val payload = json.decodeFromString<UpdateStickerPackRequest>(operation.payload)
        assertEquals("public", payload.visibility)
    }

    @Test
    fun normalizePackVisibilityForApiUsesLowercaseValues() {
        assertEquals("public", normalizePackVisibilityForApi("PUBLIC"))
        assertEquals("private", normalizePackVisibilityForApi("private"))
        assertEquals("unlisted", normalizePackVisibilityForApi("UNLISTED"))
    }

    @Test
    fun shouldReplaceLocalStickersOnlyAfterSuccessfulImport() {
        assertEquals(false, shouldReplaceLocalStickersFromRemote(importedStickerCount = 0))
        assertEquals(true, shouldReplaceLocalStickersFromRemote(importedStickerCount = 3))
    }

    @Test
    fun shouldPreserveLocalStickersForExistingPackWithContent() {
        assertEquals(false, shouldPreserveLocalStickersOnPull(hasExistingPack = false, localStickerCount = 5))
        assertEquals(false, shouldPreserveLocalStickersOnPull(hasExistingPack = true, localStickerCount = 0))
        assertEquals(true, shouldPreserveLocalStickersOnPull(hasExistingPack = true, localStickerCount = 4))
    }

    @Test
    fun shouldImportStickersIntoExistingPackOnlyWhenLocalIsEmpty() {
        assertEquals(false, shouldImportStickersIntoExistingPack(localStickerCount = 5, remoteStickerCount = 0))
        assertEquals(false, shouldImportStickersIntoExistingPack(localStickerCount = 3, remoteStickerCount = 3))
        assertEquals(true, shouldImportStickersIntoExistingPack(localStickerCount = 0, remoteStickerCount = 4))
        assertEquals(false, shouldImportStickersIntoExistingPack(localStickerCount = 0, remoteStickerCount = 0))
    }

    @Test
    fun shouldIgnoreRemoteStickerDeletionForCloudLinkedLocalPacks() {
        val linked = StickerPackEntity(
            identifier = "local-pack",
            name = "Pack",
            publisher = "Setiker",
            trayImageFile = "tray.webp",
            cloudId = "cloud-pack",
        )
        assertEquals(true, shouldIgnoreRemoteStickerDeletion(linked))
        assertEquals(false, shouldIgnoreRemoteStickerDeletion(null))
        assertEquals(
            false,
            shouldIgnoreRemoteStickerDeletion(
                linked.copy(cloudId = null),
            ),
        )
    }

    @Test
    fun operationIsPackVisibilityUpdateMatchesVisibilityOperationOnly() {
        val visibilityOp = createPackVisibilitySyncOperation(
            cloudPackId = "cloud-pack-1",
            visibility = "PRIVATE",
            id = "op-visibility",
            createdAt = 3L,
        )
        val deleteStickerOp = createDeleteStickerSyncOperation(
            cloudPackId = "cloud-pack-1",
            cloudStickerId = "cloud-sticker-1",
            id = "op-delete",
            createdAt = 4L,
        )

        assertEquals(true, operationIsPackVisibilityUpdate(visibilityOp))
        assertEquals(false, operationIsPackVisibilityUpdate(deleteStickerOp))
    }

    @Test
    fun operationBelongsToPackMatchesLocalAndCloudTargets() {
        val visibilityOp = createPackVisibilitySyncOperation(
            cloudPackId = "cloud-pack-1",
            visibility = "PUBLIC",
            id = "op-visibility",
            createdAt = 1L,
        )
        val deleteStickerOp = createDeleteStickerSyncOperation(
            cloudPackId = "cloud-pack-1",
            cloudStickerId = "cloud-sticker-1",
            id = "op-delete",
            createdAt = 2L,
        )

        assertEquals(
            true,
            operationBelongsToPack(visibilityOp, localPackId = "local-pack", cloudPackId = "cloud-pack-1"),
        )
        assertEquals(
            true,
            operationBelongsToPack(deleteStickerOp, localPackId = "local-pack", cloudPackId = "cloud-pack-1"),
        )
        assertEquals(
            false,
            operationBelongsToPack(visibilityOp, localPackId = "local-pack", cloudPackId = "cloud-pack-2"),
        )
    }

    @Test
    fun createDeleteStickerSyncOperationUsesCloudIds() {
        val operation = createDeleteStickerSyncOperation(
            cloudPackId = "cloud-pack-1",
            cloudStickerId = "cloud-sticker-1",
            id = "op-delete",
            createdAt = 99L,
        )

        assertEquals(SyncOperationType.DELETE_STICKER, operation.type)
        assertEquals("cloud-sticker-1", operation.targetId)
        val payload = json.decodeFromString<DeleteStickerSyncPayload>(operation.payload)
        assertEquals("cloud-pack-1", payload.stickerPackId)
        assertEquals("cloud-sticker-1", payload.stickerId)
    }

    @Test
    fun existingCloudPackStillUsesFullUpdateOperation() {
        val existing = StickerPackEntity(
            identifier = "local-pack",
            name = "Cloud Pack",
            publisher = "Setiker",
            trayImageFile = "tray.webp",
            cloudId = "cloud-pack",
            cloudOwnerId = "user-1",
        )

        val target = resolvePackSaveSyncTarget(
            existing = existing,
            localIdentifier = "local-pack",
            currentUserId = "user-1",
        )

        assertEquals(SyncOperationType.UPDATE_PACK, target.type)
        assertEquals("cloud-pack", target.targetId)
    }
}
