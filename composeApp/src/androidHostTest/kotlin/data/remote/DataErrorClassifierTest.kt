package data.remote

import data.sync.ConnectivityState
import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.detailText
import domain.error.isRateLimited
import domain.error.isServerError
import domain.error.isUnauthorized
import io.ktor.client.network.sockets.ConnectTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Pins the typed retry classification contract (P3b, D1) that replaced the broken
 * string-matching classifier (`contains("timeout")`, `contains("5")`) in the sync executor.
 */
class DataErrorClassifierTest {

    @Test
    fun `connect timeout is retryable`() {
        val out = ConnectTimeoutException("connect timeout").toDataError()
        assertIs<DataError.Network.ConnectTimeout>(out)
        assertTrue(out.retryable)
    }

    @Test
    fun `http 405 is permanent and 503 is retryable`() {
        assertFalse(DataError.Network.HttpFailure(status = 405).retryable)
        assertTrue(DataError.Network.HttpFailure(status = 503).retryable)
        assertTrue(DataError.Network.HttpFailure(status = 429).retryable)
        assertFalse(DataError.Network.HttpFailure(status = null).retryable)
    }

    @Test
    fun `api rejection carries appCode and is not retryable`() {
        val out = ApiException(AppErrorCode.CloudUpdateFailed, "nope").toDataError()
        val rejected = assertIs<DataError.Network.ApiRejected>(out)
        assertEquals(AppErrorCode.CloudUpdateFailed, rejected.appCode)
        assertFalse(rejected.retryable)
    }

    @Test
    fun `io exception maps to NoInternet and is retryable`() {
        val out = java.io.IOException("net down").toDataError()
        assertIs<DataError.Network.NoInternet>(out)
        assertTrue(out.retryable)
    }

@Test
    fun `a failed connection while the device is online is not an offline device`() {
        // The phone is online and the server could not be reached. Calling that "offline" is
        // false, and it sends the user looking at the wrong thing.
        ConnectivityState.update(true)
        try {
            val out = java.io.IOException("connection refused").toDataError()
            assertIs<DataError.Network.ServerUnreachable>(out)
            assertTrue(out.retryable)
        } finally {
            ConnectivityState.update(false)
        }
    }

    @Test
    fun `a connect timeout while the device is online is an unreachable server`() {
        ConnectivityState.update(true)
        try {
            val out = ConnectTimeoutException("connect timeout").toDataError()
            assertIs<DataError.Network.ServerUnreachable>(out)
        } finally {
            ConnectivityState.update(false)
        }
    }

    @Test
    fun `legacy false-retryable case is now permanent unknown`() {
        // "slice 512 bytes" previously matched contains("5") and was marked retryable.
        val out = RuntimeException("slice 512 bytes").toDataError()
        assertIs<DataError.Network.Unknown>(out)
        assertFalse(out.retryable)
    }

    @Test
    fun `detail text falls back to HTTP status`() {
        assertEquals("HTTP 405", DataError.Network.HttpFailure(status = 405).detailText())
        assertEquals("Network unavailable", DataError.Network.NoInternet.detailText())
    }

    @Test
    fun `status predicates`() {
        val unauthorized = DataError.Network.HttpFailure(status = 401)
        val rateLimited = DataError.Network.HttpFailure(status = 429, appCode = AppErrorCode.AiQuotaExceeded)
        val server = DataError.Network.HttpFailure(status = 502)
        assertTrue(unauthorized.isUnauthorized())
        assertTrue(rateLimited.isRateLimited())
        assertTrue(server.isServerError())
        assertFalse(unauthorized.isServerError())
    }
}
