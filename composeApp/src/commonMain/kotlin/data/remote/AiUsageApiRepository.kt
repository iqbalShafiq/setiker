package data.remote

import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import data.remote.model.AiQuotaFinalizeRequestDto
import data.remote.model.AiQuotaReserveRequestDto
import data.remote.model.AiQuotaReserveResponseDto
import data.remote.model.AiUsageCountsDto
import data.remote.model.AiUsageDto
import data.remote.model.ApiSuccessEnvelope
import domain.error.AppErrorCode
import domain.model.AiQuotaOperation
import domain.model.AiQuotaReservation
import domain.model.AiUsage
import domain.model.AiUsageCounts
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import presentation.common.apiName

class AiUsageApiRepository(
    private val authManager: AuthManager,
    private val authTokenRefresher: AuthTokenRefresher? = null,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun getUsage(): AiUsage {
        val response = withAuthRetry { token ->
            client.get("$baseUrl/api/v1/ai/usage") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        return parseSuccess(response) { bodyText ->
            json.decodeFromString<ApiSuccessEnvelope<AiUsageDto>>(bodyText).data?.toDomain()
                ?: throw ApiException(AppErrorCode.CloudFetchFailed)
        }
    }

    suspend fun reserve(operation: AiQuotaOperation): AiQuotaReservation {
        val response = withAuthRetry { token ->
            client.post("$baseUrl/api/v1/ai/quota/reserve") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(AiQuotaReserveRequestDto(operation = operation.apiName()))
            }
        }
        return parseSuccess(response) { bodyText ->
            val dto = json.decodeFromString<ApiSuccessEnvelope<AiQuotaReserveResponseDto>>(bodyText).data
                ?: throw ApiException(AppErrorCode.CloudFetchFailed)
            AiQuotaReservation(
                reservationId = dto.reservationId,
                operation = operation,
                pointCost = dto.pointCost,
                pointsRemaining = dto.pointsRemaining,
                reservationExpiresAt = dto.reservationExpiresAt,
                serverNow = dto.serverNow
            )
        }
    }

    suspend fun finalize(reservationId: String, committed: Boolean) {
        val response = withAuthRetry { token ->
            client.post("$baseUrl/api/v1/ai/quota/finalize") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AiQuotaFinalizeRequestDto(
                        reservationId = reservationId,
                        outcome = if (committed) "committed" else "released"
                    )
                )
            }
        }
        parseSuccess(response) { }
    }

    private suspend inline fun <T> parseSuccess(
        response: io.ktor.client.statement.HttpResponse,
        crossinline onSuccess: (String) -> T
    ): T {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val parsed = ApiErrorParser.parse(bodyText)
            throw ApiException(
                code = parsed?.code ?: AppErrorCode.CloudFetchFailed,
                message = parsed?.message
            )
        }
        return onSuccess(bodyText)
    }

    private suspend fun withAuthRetry(
        request: suspend (String) -> io.ktor.client.statement.HttpResponse
    ): io.ktor.client.statement.HttpResponse {
        val firstToken = resolveAccessToken() ?: throw ApiException(AppErrorCode.AuthNotAuthenticated)
        val firstResponse = request(firstToken)
        if (firstResponse.status != HttpStatusCode.Unauthorized) return firstResponse
        val refreshed = forceRefreshAccessToken() ?: return firstResponse
        return request(refreshed)
    }

    private suspend fun resolveAccessToken(): String? {
        authManager.getValidAccessToken()?.let { return it }
        return authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = false)
            ?: authManager.getAccessToken()
    }

    private suspend fun forceRefreshAccessToken(): String? {
        val refreshed = authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = true)
        return if (refreshed.isNullOrBlank()) null else refreshed
    }
}

private fun AiUsageDto.toDomain(): AiUsage {
    val costs = operationCosts.toDomain()
    val limit = pointLimit.takeIf { it > 0 }
        ?: limits.generate + limits.gridSplit + limits.backgroundRemove +
            limits.videoStickerPack + limits.improve
    val used = pointsUsed.takeIf { pointLimit > 0 }
        ?: used.generate + used.gridSplit + used.backgroundRemove +
            used.videoStickerPack + used.improve
    val outstanding = pointsOutstanding
    val remaining = pointsRemaining.takeIf { pointLimit > 0 }
        ?: (limit - used - outstanding).coerceAtLeast(0)
    return AiUsage(
        period = period,
        periodStart = periodStart,
        periodEnd = periodEnd,
        pointLimit = limit,
        effectiveDailyLimit = effectiveDailyLimit.takeIf { it > 0 } ?: limit,
        pointsUsed = used,
        pointsOutstanding = outstanding,
        pointsRemaining = remaining,
        purchasedTokenBalance = purchasedTokenBalance,
        subscriptionTier = subscriptionTier,
        resetTimezone = resetTimezone,
        quotaSource = quotaSource,
        operationCosts = costs,
        resetsAt = resetsAt,
        serverNow = serverNow
    )
}

private fun AiUsageCountsDto.toDomain(): AiUsageCounts = AiUsageCounts(
    generate = generate,
    gridSplit = gridSplit,
    backgroundRemove = backgroundRemove,
    videoStickerPack = videoStickerPack,
    improve = improve,
    packImport = packImport
)
