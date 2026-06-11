package data.repository

import data.local.entity.StickerPackEntity
import data.local.entity.StickerEntity
import data.sync.createPackSyncOperation
import data.sync.resolvePackSaveSyncTarget
import domain.model.SyncOperationType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class StickerRepositorySyncTest {

    @Test
    fun newLocalPackUsesCreateOperationWithLocalIdentifier() {
        val target = resolvePackSaveSyncTarget(existing = null, localIdentifier = "local-pack")

        assertEquals(SyncOperationType.CREATE_PACK, target.type)
        assertEquals("local-pack", target.targetId)
    }

    @Test
    fun existingLocalOnlyPackStillUsesCreateOperationWithLocalIdentifier() {
        val existing = StickerPackEntity(
            identifier = "local-pack",
            name = "Local Pack",
            publisher = "Setiker",
            trayImageFile = "tray.webp",
            cloudId = null,
        )

        val target = resolvePackSaveSyncTarget(existing, localIdentifier = "local-pack")

        assertEquals(SyncOperationType.CREATE_PACK, target.type)
        assertEquals("local-pack", target.targetId)
    }

    @Test
    fun existingCloudPackUsesUpdateOperationWithCloudId() {
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

    @Test
    fun foreignCloudLinkedPackFallsBackToCreateOperation() {
        val existing = StickerPackEntity(
            identifier = "imported-pack",
            name = "Imported Pack",
            publisher = "Other",
            trayImageFile = "tray.webp",
            cloudId = "cloud-pack-other",
            cloudOwnerId = "user-2",
        )

        val target = resolvePackSaveSyncTarget(
            existing = existing,
            localIdentifier = "imported-pack",
            currentUserId = "user-1",
        )

        assertEquals(SyncOperationType.CREATE_PACK, target.type)
        assertEquals("imported-pack", target.targetId)
    }

    @Test
    fun localOnlyPackCanBeConvertedToCreateSyncOperation() {
        val pack = StickerPackEntity(
            identifier = "local-pack",
            name = "Local Pack",
            publisher = "Tester",
            trayImageFile = "tray.webp",
            cloudId = null,
        )
        val stickers = listOf(
            StickerEntity(
                id = "sticker-1",
                packId = "local-pack",
                imageFile = "sticker.webp",
                emojis = "[]",
                accessibilityText = null,
                sortOrder = 0,
            )
        )

        val operation = createPackSyncOperation(pack, stickers, id = "op-1", createdAt = 10L)

        assertEquals(SyncOperationType.CREATE_PACK, operation.type)
        assertEquals("local-pack", operation.targetId)
        assertEquals("Local Pack", Json.parseToJsonElement(operation.payload).jsonObject["name"]?.jsonPrimitive?.content)
    }
}
