package domain.model

data class AiUsageCounts(
    val generate: Int = 0,
    val gridSplit: Int = 0,
    val backgroundRemove: Int = 0,
    val videoStickerPack: Int = 0,
    val improve: Int = 0,
    val packImport: Int = 0
)

data class AiUsage(
    val period: String = "daily",
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val pointLimit: Int = 0,
    val effectiveDailyLimit: Int = 0,
    val pointsUsed: Int = 0,
    val pointsOutstanding: Int = 0,
    val pointsRemaining: Int = 0,
    val purchasedTokenBalance: Int = 0,
    val subscriptionTier: String = "free",
    val resetTimezone: String? = null,
    val quotaSource: String? = null,
    val operationCosts: AiUsageCounts = AiUsageCounts(),
    val resetsAt: String? = null,
    val serverNow: String? = null
) {
    fun costFor(operation: AiQuotaOperation): Int = when (operation) {
        AiQuotaOperation.GENERATE -> operationCosts.generate
        AiQuotaOperation.GRID_SPLIT -> operationCosts.gridSplit
        AiQuotaOperation.BACKGROUND_REMOVE -> operationCosts.backgroundRemove
        AiQuotaOperation.VIDEO_STICKER_PACK -> operationCosts.videoStickerPack
        AiQuotaOperation.IMPROVE -> operationCosts.improve
        AiQuotaOperation.PACK_IMPORT -> operationCosts.packImport
    }
}

enum class AiQuotaOperation {
    GENERATE,
    GRID_SPLIT,
    BACKGROUND_REMOVE,
    VIDEO_STICKER_PACK,
    IMPROVE,
    PACK_IMPORT
}

data class AiQuotaReservation(
    val reservationId: String,
    val operation: AiQuotaOperation,
    val pointCost: Int,
    val pointsRemaining: Int,
    val reservationExpiresAt: String? = null,
    val serverNow: String? = null
)
