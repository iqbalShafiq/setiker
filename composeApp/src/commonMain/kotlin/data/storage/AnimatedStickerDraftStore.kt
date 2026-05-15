package data.storage

import domain.model.AnimatedStickerSpec
import domain.model.DecodedFrame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Clock

/**
 * In-memory transient store for animated sticker drafts. Used to hand off heavy
 * decoded-frame bytes between the trim screen and the animated editor without bloating
 * navigation arguments.
 */
class AnimatedStickerDraftStore {

    data class Draft(
        val videoPath: String,
        val frames: List<DecodedFrame>,
        val spec: AnimatedStickerSpec
    )

    private val mutex = Mutex()
    private val drafts: MutableMap<String, Draft> = mutableMapOf()

    suspend fun put(draft: Draft): String = mutex.withLock {
        val id = "anim_draft_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100000)}"
        drafts[id] = draft
        id
    }

    suspend fun get(id: String): Draft? = mutex.withLock {
        drafts[id]
    }

    suspend fun release(id: String): Draft? = mutex.withLock {
        drafts.remove(id)
    }

    suspend fun clearAll() = mutex.withLock {
        drafts.clear()
    }
}
