package data.sync

import kotlinx.coroutines.sync.Mutex

class SyncRunGate {
    private val mutex = Mutex()

    fun tryEnter(): Boolean = mutex.tryLock()

    fun leave() {
        mutex.unlock()
    }
}
