package data.remote.explore

import data.remote.ApiException
import data.remote.json.ApiJson
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.BlockedUsersData
import data.remote.model.ReportContentRequest
import domain.error.AppErrorCode
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Moderation endpoints: content reports and user blocking.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5).
 */
internal class ModerationDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun reportPack(packId: String, reason: String, details: String? = null) {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.post("${gateway.baseUrl}/api/v1/sticker-packs/$packId/report") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(ReportContentRequest(reason = reason, details = details))
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(response.bodyAsText()))
        }
    }

    suspend fun reportProcessingHistory(historyId: String, reason: String, details: String? = null) {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.post("${gateway.baseUrl}/api/v1/processing-history/$historyId/report") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(ReportContentRequest(reason = reason, details = details))
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(response.bodyAsText()))
        }
    }

    suspend fun blockUser(userId: String) {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.post("${gateway.baseUrl}/api/v1/users/$userId/block") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(response.bodyAsText()))
        }
    }

    suspend fun listBlockedUsers(): BlockedUsersData {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.get("${gateway.baseUrl}/api/v1/users/blocked") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<BlockedUsersData>>(bodyText).data
            ?: BlockedUsersData()
    }

    suspend fun unblockUser(userId: String) {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.delete("${gateway.baseUrl}/api/v1/users/$userId/block") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = gateway.extractMessage(response.bodyAsText()))
        }
    }
}
