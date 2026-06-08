package data.sync

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncRunGateTest {

    @Test
    fun withLockSerializesConcurrentSyncRuns() = runTest {
        val gate = SyncRunGate()
        val order = mutableListOf<Int>()

        val first = async {
            gate.withLock {
                order += 1
                order += 2
            }
        }
        val second = async {
            gate.withLock {
                order += 3
            }
        }

        first.await()
        second.await()

        assertEquals(listOf(1, 2, 3), order)
    }
}
