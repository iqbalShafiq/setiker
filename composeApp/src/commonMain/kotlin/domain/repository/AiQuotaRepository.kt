package domain.repository

import domain.model.AiQuotaOperation
import domain.model.AiQuotaReservation
import domain.model.AiUsage

interface AiQuotaRepository {
    suspend fun getUsage(forceRefresh: Boolean = false): AiUsage?
    suspend fun reserve(operation: AiQuotaOperation): AiQuotaReservation
    suspend fun finalizeCommitted(reservationId: String)
    suspend fun finalizeReleased(reservationId: String)
    fun invalidateCache()
}
