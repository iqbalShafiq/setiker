package data.sync

import data.local.entity.StickerPackEntity
import data.remote.model.UpdateStickerPackRequest
import domain.model.SyncOperationType
import kotlinx.serialization.json.Json
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
    fun existingCloudPackStillUsesFullUpdateOperation() {
        val existing = StickerPackEntity(
            identifier = "local-pack",
            name = "Cloud Pack",
            publisher = "Setiker",
            trayImageFile = "tray.webp",
            cloudId = "cloud-pack",
        )

        val target = resolvePackSaveSyncTarget(existing, localIdentifier = "local-pack")

        assertEquals(SyncOperationType.UPDATE_PACK, target.type)
        assertEquals("cloud-pack", target.targetId)
    }
}
