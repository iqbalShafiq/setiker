package data.remote.explore

import data.remote.ApiException
import data.remote.runApiCall
import data.remote.json.ApiJson
import data.remote.model.AcceptSharedPackData
import data.remote.model.AcceptSharedStickerData
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.CloudStickerPackShareLink
import data.remote.model.CreateStickerPackLinkRequest
import data.remote.model.SharePreviewPackData
import data.remote.model.SharePreviewStickerData
import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
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
 * Pack/sticker share links and share-token acceptance.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5). Typed Result per P3b D6/Family B.
 */
internal class PackShareLinkDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun previewPackShare(token: String): Result<SharePreviewPackData, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Public) { _ ->
                gateway.http.get("${gateway.baseUrl}/api/v1/share/pack/$token")
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<SharePreviewPackData>>(bodyText).data
                ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
        }

    suspend fun previewStickerShare(token: String): Result<SharePreviewStickerData, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Public) { _ ->
                gateway.http.get("${gateway.baseUrl}/api/v1/share/sticker/$token")
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<SharePreviewStickerData>>(bodyText).data
                ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
        }

    suspend fun acceptPackShare(token: String): Result<AcceptSharedPackData, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.post("${gateway.baseUrl}/api/v1/share/pack/$token/accept") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudCreateFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<AcceptSharedPackData>>(bodyText).data
                ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }

    suspend fun acceptStickerShare(token: String): Result<AcceptSharedStickerData, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.post("${gateway.baseUrl}/api/v1/share/sticker/$token/accept") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudCreateFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<AcceptSharedStickerData>>(bodyText).data
                ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }

    suspend fun getPackLinks(packId: String): Result<List<CloudStickerPackShareLink>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/sticker-packs/$packId/links") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<List<CloudStickerPackShareLink>>>(bodyText).data.orEmpty()
        }

    suspend fun createPackLink(packId: String, request: CreateStickerPackLinkRequest): Result<CloudStickerPackShareLink, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.post("${gateway.baseUrl}/api/v1/sticker-packs/$packId/link") {
                    header(HttpHeaders.Authorization, authHeader)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudCreateFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<CloudStickerPackShareLink>>(bodyText).data
                ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }

    suspend fun revokePackLink(packId: String, linkId: String): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.delete("${gateway.baseUrl}/api/v1/sticker-packs/$packId/link/$linkId") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = gateway.extractMessage(bodyText))
            }
        }

    private companion object {
        const val TAG = "ExplorePackShares"
    }
}
