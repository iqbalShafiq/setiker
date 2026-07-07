package data.remote

import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.LegalDocumentDto
import data.remote.model.LegalRetentionDto
import data.remote.model.LegalSectionDto
import data.remote.model.LegalSummaryDto
import domain.error.AppErrorCode
import domain.model.LegalDocument
import domain.model.LegalSection
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
            accountDeletionUrl = dto.accountDeletionUrl,
            version = dto.version,
            effectiveDate = dto.effectiveDate
        )
    }

    suspend fun getPrivacyDocument(): LegalDocument = fetchDocument("/api/v1/legal/privacy").toDomain()

    suspend fun getTermsDocument(): LegalDocument = fetchDocument("/api/v1/legal/terms").toDomain()

    suspend fun getRetentionDocument(): LegalDocument {
        val bodyText = fetchPublic("/api/v1/legal/retention")
        val dto = json.decodeFromString<ApiSuccessEnvelope<LegalRetentionDto>>(bodyText).data
            ?: throw ApiException(AppErrorCode.CloudFetchFailed)
        return LegalDocument(
            title = "Data Retention",
            summary = dto.description,
            sections = dto.sections.map { it.toDomain() }
        )
    }

    suspend fun getPermissionsDocument(): LegalDocument {
        val privacy = getPrivacyDocument()
        val permissionSections = privacy.sections.filter {
            it.id == "permissions" || it.id == "user-content"
        }
        return LegalDocument(
            title = "App Permissions",
            summary = "How Setiker uses device permissions and the system photo picker.",
            sections = if (permissionSections.isNotEmpty()) {
                permissionSections
            } else {
                listOf(
                    LegalSection(
                        id = "permissions",
                        title = "Permissions",
                        body = privacy.sections.firstOrNull { it.id == "permissions" }?.body
                            ?: "Setiker requests notifications only for AI job progress and uses the system picker for photos and videos."
                    )
                )
            }
        )
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

private fun LegalDocumentDto.toDomain() = LegalDocument(
    title = title,
    version = version,
    effectiveDate = effectiveDate,
    url = url,
    summary = summary,
    sections = sections.map { it.toDomain() }
)

private fun LegalSectionDto.toDomain() = LegalSection(id = id, title = title, body = body)
