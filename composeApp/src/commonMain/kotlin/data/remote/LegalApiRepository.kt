package data.remote

import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.LegalDocumentDto
import data.remote.model.LegalRetentionDto
import data.remote.model.LegalSummaryDto
import domain.error.AppErrorCode
import domain.model.LegalSummary
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LegalApiRepository(
    private val authManager: AuthManager,
    private val authTokenRefresher: AuthTokenRefresher? = null,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun getSummary(): LegalSummary {
        val bodyText = fetchPublic("/api/v1/legal")
        val dto = json.decodeFromString<ApiSuccessEnvelope<LegalSummaryDto>>(bodyText).data
            ?: throw ApiException(AppErrorCode.CloudFetchFailed)
        return LegalSummary(
            privacyUrl = dto.privacyUrl,
            termsUrl = dto.termsUrl,
            retentionUrl = dto.retentionUrl,
            version = dto.version,
            effectiveDate = dto.effectiveDate
        )
    }

    suspend fun getPrivacy(): LegalDocumentDto = fetchDocument("/api/v1/legal/privacy")

    suspend fun getTerms(): LegalDocumentDto = fetchDocument("/api/v1/legal/terms")

    suspend fun getRetention(): LegalRetentionDto {
        val bodyText = fetchPublic("/api/v1/legal/retention")
        return json.decodeFromString<ApiSuccessEnvelope<LegalRetentionDto>>(bodyText).data
            ?: throw ApiException(AppErrorCode.CloudFetchFailed)
    }

    private suspend fun fetchDocument(path: String): LegalDocumentDto {
        val bodyText = fetchPublic(path)
        return json.decodeFromString<ApiSuccessEnvelope<LegalDocumentDto>>(bodyText).data
            ?: throw ApiException(AppErrorCode.CloudFetchFailed)
    }

    private suspend fun fetchPublic(path: String): String {
        val response = client.get("$baseUrl$path") {
            resolveAccessToken()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val parsed = ApiErrorParser.parse(bodyText)
            throw ApiException(
                code = parsed?.code ?: AppErrorCode.CloudFetchFailed,
                message = parsed?.message
            )
        }
        return bodyText
    }

    private suspend fun resolveAccessToken(): String? {
        authManager.getValidAccessToken()?.let { return it }
        return authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = false)
            ?: authManager.getAccessToken()
    }
}
