package data.remote.explore

import data.remote.ApiException
import data.remote.json.ApiJson
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.CloudStickerShareLink
import data.remote.model.CreateStickerPackLinkRequest
import data.remote.model.SharePackWithUserRequest
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
 * Sticker-level sharing: links, direct share to users, and sticker collaborators.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5).
 */
internal class StickerShareLinkDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun getStickerLinks(stickerId: String): List<CloudStickerShareLink> {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.get("${gateway.baseUrl}/api/v1/stickers/$stickerId/links") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<CloudStickerShareLink>>>(bodyText).data.orEmpty()
    }

    suspend fun createStickerLink(stickerId: String, request: CreateStickerPackLinkRequest): CloudStickerShareLink {
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
        return json.decodeFromString<ApiSuccessEnvelope<CloudStickerShareLink>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun revokeStickerLink(stickerId: String, linkId: String) {
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

    suspend fun shareStickerWithUser(stickerId: String, request: SharePackWithUserRequest) {
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

    suspend fun removeStickerShare(stickerId: String, userId: String) {
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

    suspend fun listStickerCollaborators(stickerId: String): List<data.remote.model.StickerCollaborator> {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.get("${gateway.baseUrl}/api/v1/stickers/$stickerId/collaborators") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<data.remote.model.StickerCollaborator>>>(bodyText)
            .data.orEmpty()
    }
}

@kotlinx.serialization.Serializable
internal data class RemovePackShareBody(val userId: String)
