package data.remote.explore

import data.remote.ApiException
import data.remote.json.ApiJson
import data.remote.model.ApiPaginatedSuccessEnvelope
import data.remote.model.UserNotificationItem
import data.remote.runApiCall
import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/**
 * Notification center endpoints.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5). Typed Result per P3b D6/Family B.
 */
internal class NotificationDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun getNotifications(page: Int, limit: Int, unreadOnly: Boolean = false): Result<Pair<List<UserNotificationItem>, Int>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/notifications") {
                    header(HttpHeaders.Authorization, authHeader)
                    parameter("page", page)
                    parameter("limit", limit)
                    if (unreadOnly) parameter("unreadOnly", true)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            val envelope = json.decodeFromString<ApiPaginatedSuccessEnvelope<List<UserNotificationItem>>>(bodyText)
            envelope.data.orEmpty() to (envelope.meta?.unreadCount ?: 0)
        }

    suspend fun markNotificationRead(id: String): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.patch("${gateway.baseUrl}/api/v1/notifications/$id/read") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = gateway.extractMessage(response.bodyAsText()))
            }
        }

    suspend fun markAllNotificationsRead(): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.post("${gateway.baseUrl}/api/v1/notifications/read-all") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = gateway.extractMessage(response.bodyAsText()))
            }
        }

    private companion object {
        const val TAG = "ExploreNotifications"
    }
}
