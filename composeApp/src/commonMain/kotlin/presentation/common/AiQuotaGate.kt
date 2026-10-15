package presentation.common

import domain.model.AiQuotaOperation
import domain.model.AiUsage
import domain.repository.AiQuotaRepository
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_ai_quota_exceeded

object AiQuotaGate {
    suspend fun checkCanStart(
        quotaRepository: AiQuotaRepository,
        operation: AiQuotaOperation,
        forceRefresh: Boolean = false
    ): UiText? {
        val usage = quotaRepository.getUsage(forceRefresh) ?: return null
        val cost = usage.costFor(operation)
        if (cost <= 0) return null
        if (usage.pointsRemaining < cost && usage.purchasedTokenBalance < cost) {
            return UiText.StringRes(Res.string.error_ai_quota_exceeded)
        }
        return null
    }

    fun canAfford(usage: AiUsage?, operation: AiQuotaOperation): Boolean {
        if (usage == null) return true
        val cost = usage.costFor(operation)
        if (cost <= 0) return true
        return usage.pointsRemaining >= cost || usage.purchasedTokenBalance >= cost
    }
}
