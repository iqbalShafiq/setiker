package presentation.common

import domain.model.AiUsage
import domain.model.AiUsageCounts
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiQuotaGateTest {
    @Test
    fun canAfford_whenRemainingCoversCost() {
        val usage = AiUsage(
            pointLimit = 100,
            pointsUsed = 10,
            pointsOutstanding = 5,
            pointsRemaining = 85,
            operationCosts = AiUsageCounts(generate = 1)
        )
        assertTrue(AiQuotaGate.canAfford(usage, domain.model.AiQuotaOperation.GENERATE))
    }

    @Test
    fun canAfford_whenPurchasedTokensCoverCost() {
        val usage = AiUsage(
            pointLimit = 100,
            pointsUsed = 100,
            pointsOutstanding = 0,
            pointsRemaining = 0,
            purchasedTokenBalance = 10,
            operationCosts = AiUsageCounts(generate = 1)
        )
        assertTrue(AiQuotaGate.canAfford(usage, domain.model.AiQuotaOperation.GENERATE))
    }

    @Test
    fun cannotAfford_whenRemainingBelowCost() {
        val usage = AiUsage(
            pointLimit = 100,
            pointsUsed = 100,
            pointsOutstanding = 0,
            pointsRemaining = 0,
            operationCosts = AiUsageCounts(generate = 1)
        )
        assertFalse(AiQuotaGate.canAfford(usage, domain.model.AiQuotaOperation.GENERATE))
    }
}
