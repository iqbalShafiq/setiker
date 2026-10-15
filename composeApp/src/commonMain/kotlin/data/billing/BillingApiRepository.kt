package data.billing

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import data.remote.model.ApiSuccessEnvelope
import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import data.remote.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

@Serializable
data class BillingProductDto(
    val code: String,
    val type: String,
    val name: String,
    val description: String? = null,
    val tokenAmount: Int? = null,
    val tierCode: String? = null,
    val dailyPointLimit: Int? = null,
    val googlePlayProductId: String? = null,
    val appleProductId: String? = null,
    val sortOrder: Int = 0
)

@Serializable
data class BillingProductsResponseDto(
    val products: List<BillingProductDto> = emptyList()
)

@Serializable
data class GooglePlayVerifyRequestDto(
    val productCode: String,
    val productId: String,
    val purchaseToken: String,
    val packageName: String
)

@Serializable
data class PurchaseVerifyResponseDto(
    val purchaseId: String,
    val status: String,
    val productCode: String,
    val type: String,
    val tokenAmountCredited: Int? = null,
    val purchasedTokenBalance: Int = 0,
    val subscriptionTier: String = "free",
    val effectiveDailyLimit: Int = 100,
    val shouldConsume: Boolean = false,
    val shouldAcknowledge: Boolean = false
)

@Serializable
data class AppleVerifyRequestDto(
    val productCode: String,
    val productId: String,
    val signedTransactionInfo: String,
    val transactionId: String? = null,
    val originalTransactionId: String? = null
)

@Serializable
data class RestorePurchasesRequestDto(
    val googlePlay: List<GooglePlayVerifyRequestDto> = emptyList(),
    val apple: List<AppleVerifyRequestDto> = emptyList()
)

@Serializable
data class SubscriptionSnapshotDto(
    val active: Boolean = false,
    val productCode: String? = null,
    val tierCode: String = "free",
    val status: String? = null,
    val currentPeriodEnd: String? = null,
    val autoRenewing: Boolean = false,
    val cancelAtPeriodEnd: Boolean = false
)

@Serializable
data class PurchaseHistoryItemDto(
    val id: String,
    val productCode: String,
    val productName: String? = null,
    val status: String,
    val type: String,
    val tokenAmount: Int? = null,
    val provider: String,
    val createdAt: String
)

@Serializable
data class PurchasesListResponseDto(
    val purchases: List<PurchaseHistoryItemDto> = emptyList(),
    val limit: Int = 20,
    val offset: Int = 0,
    val hasMore: Boolean = false
)

data class PurchasesPageResult(
    val purchases: List<PurchaseHistoryItemDto>,
    val hasMore: Boolean
)

class BillingApiRepository(
    private val authManager: AuthManager,
    private val authTokenRefresher: AuthTokenRefresher? = null,
    private val baseUrl: String = ApiConfig.baseUrl,
    private val packageName: String = "com.setiker.app"
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun fetchProducts(): List<BillingProductDto> {
        val response = client.get("$baseUrl/api/v1/billing/products")
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw BillingApiException("Failed to load products: ${response.status}")
        }
        return json.decodeFromString<ApiSuccessEnvelope<BillingProductsResponseDto>>(body)
            .data?.products.orEmpty()
    }

    suspend fun listPurchases(limit: Int = 20, offset: Int = 0): PurchasesPageResult {
        val response = withAuthRetry { token ->
            client.get("$baseUrl/api/v1/billing/purchases") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("limit", limit)
                parameter("offset", offset)
            }
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw BillingApiException("Failed to load purchases: ${response.status}")
        }
        val data = json.decodeFromString<ApiSuccessEnvelope<PurchasesListResponseDto>>(body).data
            ?: PurchasesListResponseDto()
        return PurchasesPageResult(
            purchases = data.purchases,
            hasMore = data.hasMore
        )
    }

    suspend fun verifyGooglePlayPurchase(
        productCode: String,
        productId: String,
        purchaseToken: String
    ): PurchaseVerifyResponseDto {
        val response = withAuthRetry { token ->
            client.post("$baseUrl/api/v1/billing/google-play/verify") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    GooglePlayVerifyRequestDto(
                        productCode = productCode,
                        productId = productId,
                        purchaseToken = purchaseToken,
                        packageName = packageName
                    )
                )
            }
        }
        return parseSuccess(response) { bodyText ->
            json.decodeFromString<ApiSuccessEnvelope<PurchaseVerifyResponseDto>>(bodyText).data
                ?: throw BillingApiException("Verification failed")
        }
    }

    suspend fun getSubscriptionSnapshot(): SubscriptionSnapshotDto {
        val response = withAuthRetry { token ->
            client.get("$baseUrl/api/v1/billing/subscription/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        return parseSuccess(response) { bodyText ->
            json.decodeFromString<ApiSuccessEnvelope<SubscriptionSnapshotDto>>(bodyText).data
                ?: SubscriptionSnapshotDto()
        }
    }

    suspend fun verifyApplePurchase(
        productCode: String,
        productId: String,
        signedTransactionInfo: String,
        transactionId: String? = null,
        originalTransactionId: String? = null
    ): PurchaseVerifyResponseDto {
        val response = withAuthRetry { token ->
            client.post("$baseUrl/api/v1/billing/apple/verify") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AppleVerifyRequestDto(
                        productCode = productCode,
                        productId = productId,
                        signedTransactionInfo = signedTransactionInfo,
                        transactionId = transactionId,
                        originalTransactionId = originalTransactionId
                    )
                )
            }
        }
        return parseSuccess(response) { bodyText ->
            json.decodeFromString<ApiSuccessEnvelope<PurchaseVerifyResponseDto>>(bodyText).data
                ?: throw BillingApiException("Verification failed")
        }
    }

    suspend fun restorePurchases(
        googlePlay: List<GooglePlayVerifyRequestDto> = emptyList(),
        apple: List<AppleVerifyRequestDto> = emptyList()
    ): Int {
        val response = withAuthRetry { token ->
            client.post("$baseUrl/api/v1/billing/restore") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(RestorePurchasesRequestDto(googlePlay = googlePlay, apple = apple))
            }
        }
        return parseSuccess(response) { bodyText ->
            val restored = json.decodeFromString<ApiSuccessEnvelope<Map<String, Int>>>(bodyText).data
            restored?.get("restored") ?: 0
        }
    }

    suspend fun restoreGooglePlayPurchases(
        items: List<GooglePlayVerifyRequestDto>
    ): Int = restorePurchases(googlePlay = items)

    private suspend inline fun <T> parseSuccess(
        response: io.ktor.client.statement.HttpResponse,
        crossinline onSuccess: (String) -> T
    ): T {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw BillingApiException("Request failed: ${response.status}")
        }
        return onSuccess(bodyText)
    }

    private suspend fun withAuthRetry(
        request: suspend (String) -> io.ktor.client.statement.HttpResponse
    ): io.ktor.client.statement.HttpResponse {
        val firstToken = authManager.getValidAccessToken()
            ?: authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = false)
            ?: authManager.getAccessToken()
            ?: throw BillingApiException("Not authenticated")
        val firstResponse = request(firstToken)
        if (firstResponse.status != HttpStatusCode.Unauthorized) return firstResponse
        val refreshed = authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = true)
            ?: return firstResponse
        return request(refreshed)
    }
}

class BillingApiException(message: String) : Exception(message)
