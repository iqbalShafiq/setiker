package data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * R1 (P3b Review Focus #1): moving the AI pipeline onto the shared client factory must not
 * clamp the long-running AI/generation timeouts to the 60 s default. `SetikerApiService`
 * uses [ApiTimeoutProfile.LongOperation]; this locks the profile values that the factory
 * applies (`requestTimeoutMillis = profile.requestMillis`).
 */
class ApiTimeoutProfileTest {

    @Test
    fun longOperationPreservesSixHundredSecondTimeout() {
        assertEquals(600_000L, ApiTimeoutProfile.LongOperation.requestMillis)
    }

    @Test
    fun defaultProfileKeepsSixtySecondTimeout() {
        assertEquals(60_000L, ApiTimeoutProfile.Default.requestMillis)
    }
}
