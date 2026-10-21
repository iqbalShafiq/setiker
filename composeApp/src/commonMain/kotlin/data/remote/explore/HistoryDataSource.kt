package data.remote.explore

import data.remote.ApiException
import data.remote.json.ApiJson
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.DeleteCountData
import data.remote.model.ProcessingHistoryItem
import data.remote.model.PromptPresetDto
import data.remote.runApiCall
import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
import domain.model.PromptPreset
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/**
 * Processing history and prompt presets.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5). Typed Result per P3b D6/Family B.
 */
internal class HistoryDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun getProcessingHistory(type: String? = null): Result<List<ProcessingHistoryItem>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/processing-history") {
                    header(HttpHeaders.Authorization, authHeader)
                    if (!type.isNullOrBlank()) parameter("type", type)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<List<ProcessingHistoryItem>>>(bodyText).data.orEmpty()
        }

    suspend fun deleteHistoryItem(id: String): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.delete("${gateway.baseUrl}/api/v1/processing-history/$id") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = gateway.extractMessage(bodyText))
            }
        }

    suspend fun clearHistory(type: String? = null): Result<Int, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.delete("${gateway.baseUrl}/api/v1/processing-history") {
                    header(HttpHeaders.Authorization, authHeader)
                    if (!type.isNullOrBlank()) parameter("type", type)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<DeleteCountData>>(bodyText).data?.deletedCount ?: 0
        }

    suspend fun getPromptPresets(category: String? = null): Result<List<PromptPreset>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Public) { _ ->
                gateway.http.get("${gateway.baseUrl}/api/v1/prompt-presets") {
                    category?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("category", it) }
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<List<PromptPresetDto>>>(bodyText)
                .data
                .orEmpty()
                .map { dto ->
                    PromptPreset(
                        id = dto.id,
                        title = dto.title,
                        category = dto.category,
                        prompt = dto.prompt,
                        referenceHint = dto.referenceHint
                    )
                }
        }

    private companion object {
        const val TAG = "ExploreHistory"
    }
}
