package presentation.sync

import domain.model.SyncOperationStatus
import domain.model.SyncOperationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * F4: the Sync screen used to print raw operation types, raw UUIDs and raw HTTP client errors —
 * including internal host names, ports and timeout configuration.
 */
class SyncOperationPresentationTest {

    @Test
    fun `a raw timeout error no longer leaks the internal url`() {
        val raw = "Connect timeout has expired [url=http://13.251.98.162:8080/api/v1/upload, connect_timeout=30000 ms]"

        val sanitized = sanitizeSyncError(raw)

        assertEquals("Connect timeout has expired", sanitized)
        assertFalse(sanitized.orEmpty().contains("13.251.98.162"))
        assertFalse(sanitized.orEmpty().contains("connect_timeout"))
    }

    @Test
    fun `a bare url is removed from the message`() {
        assertEquals("Upload failed", sanitizeSyncError("Upload failed http://10.0.0.5:7845/api/v1/upload"))
    }

    @Test
    fun `a message with nothing technical left is kept`() {
        assertEquals("Network error", sanitizeSyncError("Network error"))
    }

    @Test
    fun `an empty message becomes null`() {
        assertNull(sanitizeSyncError(null))
        assertNull(sanitizeSyncError(""))
        assertNull(sanitizeSyncError("   "))
        assertNull(sanitizeSyncError("[url=http://host/path]"))
    }

    @Test
    fun `a connect timeout is classified as a timeout`() {
        assertEquals(
            SyncErrorReason.TIMEOUT,
            syncErrorReason("Connect timeout has expired [url=http://host/path, connect_timeout=30000 ms]")
        )
    }

    @Test
    fun `a refused connection is classified as a connection problem`() {
        assertEquals(
            SyncErrorReason.CONNECTION,
            syncErrorReason("java.net.ConnectException: Failed to connect to /127.0.0.1:7845")
        )
    }

    @Test
    fun `an unresolvable host is classified as a connection problem`() {
        assertEquals(
            SyncErrorReason.CONNECTION,
            syncErrorReason("java.net.UnknownHostException: api.setiker.app")
        )
    }

    @Test
    fun `an expired token is classified as an authentication problem`() {
        assertEquals(
            SyncErrorReason.AUTHENTICATION,
            syncErrorReason("UNAUTHORIZED: Token has expired")
        )
    }

    @Test
    fun `a server error is classified as a server problem`() {
        assertEquals(SyncErrorReason.SERVER, syncErrorReason("HTTP 503 Service Unavailable"))
    }

    @Test
    fun `an unknown error has no classification`() {
        assertNull(syncErrorReason("Something odd happened"))
    }

    @Test
    fun `operation types map to human titles`() {
        assertEquals(SyncOperationTypeTitle.CREATING_PACK, syncOperationTitle(SyncOperationType.CREATE_PACK))
        assertEquals(SyncOperationTypeTitle.UPDATING_PACK, syncOperationTitle(SyncOperationType.UPDATE_PACK))
        assertEquals(SyncOperationTypeTitle.DELETING_PACK, syncOperationTitle(SyncOperationType.DELETE_PACK))
        assertEquals(SyncOperationTypeTitle.ADDING_STICKER, syncOperationTitle(SyncOperationType.ADD_STICKER))
        assertEquals(SyncOperationTypeTitle.UPLOADING_IMAGE, syncOperationTitle(SyncOperationType.UPLOAD_IMAGE))
    }

    @Test
    fun `operation statuses map to human labels`() {
        assertEquals(SyncOperationStatusLabel.PENDING, syncOperationStatusLabel(SyncOperationStatus.PENDING))
        assertEquals(SyncOperationStatusLabel.RUNNING, syncOperationStatusLabel(SyncOperationStatus.IN_PROGRESS))
        assertEquals(SyncOperationStatusLabel.DONE, syncOperationStatusLabel(SyncOperationStatus.SUCCESS))
        assertEquals(SyncOperationStatusLabel.FAILED, syncOperationStatusLabel(SyncOperationStatus.FAILED))
        assertEquals(SyncOperationStatusLabel.CANCELLED, syncOperationStatusLabel(SyncOperationStatus.CANCELLED))
    }
}
