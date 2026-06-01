package data.repository

import data.remote.AiUsageApiRepository
import domain.model.AiQuotaOperation
import domain.model.AiQuotaReservation
import domain.model.AiUsage
import domain.repository.AiQuotaRepository
import kotlin.time.Clock

class AiQuotaRepositoryImpl(
    private val api: AiUsageApiRepository
) : AiQuotaRepository {
    private var cachedUsage: AiUsage? = null
    private var cachedAtMillis: Long = 0L

    override suspend fun getUsage(forceRefresh: Boolean): AiUsage? {
        val now = Clock.System.now().toEpochMilliseconds()
        if (!forceRefresh && cachedUsage != null && now - cachedAtMillis < CACHE_TTL_MS) {
            return cachedUsage
        }
        return runCatching { api.getUsage() }
            .onSuccess {
                cachedUsage = it
                cachedAtMillis = now
            }
            .getOrNull()
    }

    override suspend fun reserve(operation: AiQuotaOperation): AiQuotaReservation {
        val result = api.reserve(operation)
        invalidateCache()
        return result
    }

    override suspend fun finalizeCommitted(reservationId: String) {
        api.finalize(reservationId, committed = true)
        invalidateCache()
    }

    override suspend fun finalizeReleased(reservationId: String) {
        api.finalize(reservationId, committed = false)
        invalidateCache()
    }

    override fun invalidateCache() {
        cachedUsage = null
        cachedAtMillis = 0L
    }

    private companion object {
        const val CACHE_TTL_MS = 8_000L
    }
}
