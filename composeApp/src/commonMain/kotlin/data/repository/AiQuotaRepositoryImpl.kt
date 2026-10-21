package data.repository

import data.remote.AiUsageApiRepository
import data.remote.toApiException
import domain.error.Result
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
        return when (val result = api.getUsage()) {
            is Result.Success -> {
                cachedUsage = result.data
                cachedAtMillis = now
                result.data
            }
            is Result.Error -> null
        }
    }

    override suspend fun reserve(operation: AiQuotaOperation): AiQuotaReservation {
        val result = api.reserve(operation)
        invalidateCache()
        return when (result) {
            is Result.Success -> result.data
            // Preserves the ApiException.code chain (R4): the AI job pipeline classifies
            // quota failures by app code, not by message.
            is Result.Error -> throw result.error.toApiException()
        }
    }

    override suspend fun finalizeCommitted(reservationId: String) {
        when (val result = api.finalize(reservationId, committed = true)) {
            is Result.Success -> invalidateCache()
            is Result.Error -> throw result.error.toApiException()
        }
    }

    override suspend fun finalizeReleased(reservationId: String) {
        when (val result = api.finalize(reservationId, committed = false)) {
            is Result.Success -> invalidateCache()
            is Result.Error -> throw result.error.toApiException()
        }
    }

    override fun invalidateCache() {
        cachedUsage = null
        cachedAtMillis = 0L
    }

    private companion object {
        const val CACHE_TTL_MS = 8_000L
    }
}
