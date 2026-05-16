package data.sync

import data.local.entity.PendingSyncOperationEntity
import domain.model.SyncOperationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PullSyncMapperTest {

    @Test
    fun syncTokenDecodesToEpochMillis() {
        val token = "MjAyNC0wNi0wMVQxMjozNDo1Ni4wMDBa"

        val timestamp = decodeSyncTokenToEpochMillis(token)

        assertEquals(1_717_245_296_000L, timestamp)
    }

    @Test
    fun lastSyncAtUsesIsoTimestamp() {
        assertEquals("2024-06-01T12:34:56Z", encodeLastSyncAt(1_717_245_296_000L))
    }

    @Test
    fun pendingOrFailedOperationProtectsLocalTarget() {
        val operations = listOf(
            PendingSyncOperationEntity(
                id = "op-1",
                type = "UPDATE_PACK",
                targetId = "cloud-pack",
                payload = "{}",
                status = SyncOperationStatus.FAILED.name,
                createdAt = 1L,
            )
        )

        assertTrue(hasBlockingLocalOperation(operations, "cloud-pack"))
    }

    @Test
    fun successfulOperationDoesNotProtectLocalTarget() {
        val operations = listOf(
            PendingSyncOperationEntity(
                id = "op-1",
                type = "UPDATE_PACK",
                targetId = "cloud-pack",
                payload = "{}",
                status = SyncOperationStatus.SUCCESS.name,
                createdAt = 1L,
            )
        )

        assertFalse(hasBlockingLocalOperation(operations, "cloud-pack"))
    }

    @Test
    fun reconciliationCanTriggerImmediateProcessingWhenItCreatesOperations() {
        assertTrue(shouldProcessAfterReconciliation(createdOperations = 1, isOnline = true, isAuthenticated = true))
    }

    @Test
    fun reconciliationDoesNotProcessWhenOffline() {
        assertFalse(shouldProcessAfterReconciliation(createdOperations = 1, isOnline = false, isAuthenticated = true))
    }

    @Test
    fun authChangeCanTriggerProcessingWhenOnlineWithToken() {
        assertTrue(shouldProcessAfterAuthChange(isOnline = true, hasAccessToken = true))
    }

    @Test
    fun authChangeDoesNotTriggerProcessingWithoutToken() {
        assertFalse(shouldProcessAfterAuthChange(isOnline = true, hasAccessToken = false))
    }
}
