package data.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncRunGateTest {

    @Test
    fun secondSyncCannotEnterWhileFirstSyncIsActive() {
        val gate = SyncRunGate()

        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter())

        gate.leave()
        assertTrue(gate.tryEnter())
    }
}
