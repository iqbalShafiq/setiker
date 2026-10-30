package data.remote.explore

import data.remote.ApiException
import data.remote.json.ApiJson
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.CloudStickerShareLink
import data.remote.model.CreateStickerPackLinkRequest
import data.remote.model.SharePackWithUserRequest
import data.remote.model.StickerCollaborator
import data.remote.runApiCall
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
import kotlinx.serialization.Serializable

/**
 * Sticker-level sharing: links, direct share to users, and sticker collaborators.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5). Typed Result per P3b D6/Family B.
 */
internal class StickerShareLinkDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun getStickerLinks(stickerId: String): Result<List<CloudStickerShareLink>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/stickers/$stickerId/links") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<List<CloudStickerShareLink>>>(bodyText).data.orEmpty()
        }

    suspend fun createStickerLink(stickerId: String, request: CreateStickerPackLinkRequest): Result<CloudStickerShareLink, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.post("${gateway.baseUrl}/api/v1/stickers/$stickerId/link") {
                    header(HttpHeaders.Authorization, authHeader)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudCreateFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<CloudStickerShareLink>>(bodyText).data
                ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }

    suspend fun revokeStickerLink(stickerId: String, linkId: String): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.delete("${gateway.baseUrl}/api/v1/stickers/$stickerId/link/$linkId") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = gateway.extractMessage(bodyText))
            }
        }

    suspend fun shareStickerWithUser(stickerId: String, request: SharePackWithUserRequest): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.post("${gateway.baseUrl}/api/v1/stickers/$stickerId/share") {
                    header(HttpHeaders.Authorization, authHeader)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudCreateFailed, message = gateway.extractMessage(bodyText))
            }
        }

    suspend fun removeStickerShare(stickerId: String, userId: String): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.delete("${gateway.baseUrl}/api/v1/stickers/$stickerId/share") {
                    header(HttpHeaders.Authorization, authHeader)
                    contentType(ContentType.Application.Json)
                    setBody(RemovePackShareBody(userId))
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = gateway.extractMessage(bodyText))
            }
        }

    suspend fun listStickerCollaborators(stickerId: String): Result<List<StickerCollaborator>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/stickers/$stickerId/collaborators") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<List<StickerCollaborator>>>(bodyText).data.orEmpty()
        }

    private companion object {
        const val TAG = "ExploreStickerShares"
    }
}

@Serializable
internal data class RemovePackShareBody(val userId: String)
