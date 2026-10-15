package data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class AiUsageCountsDto(
    val generate: Int = 0,
    val gridSplit: Int = 0,
    val backgroundRemove: Int = 0,
    val videoStickerPack: Int = 0,
    val improve: Int = 0,
    val packImport: Int = 0
)

@Serializable
data class AiUsageDto(
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
    val operationCosts: AiUsageCountsDto = AiUsageCountsDto(),
    val resetsAt: String? = null,
    val serverNow: String? = null,
    val limits: AiUsageCountsDto = AiUsageCountsDto(),
    val used: AiUsageCountsDto = AiUsageCountsDto(),
    val remaining: AiUsageCountsDto = AiUsageCountsDto()
)

@Serializable
data class AiQuotaReserveRequestDto(
    val operation: String
)

@Serializable
data class AiQuotaReserveResponseDto(
    val reservationId: String,
    val operation: String,
    val pointCost: Int = 0,
    val pointLimit: Int = 0,
    val pointsUsed: Int = 0,
    val pointsOutstanding: Int = 0,
    val pointsRemaining: Int = 0,
    val resetsAt: String? = null,
    val reservationExpiresAt: String? = null,
    val serverNow: String? = null,
    val usage: AiUsageDto? = null
)

@Serializable
data class AiQuotaFinalizeRequestDto(
    val reservationId: String,
    val outcome: String
)
