package data.remote.explore

import data.remote.ApiException
import data.remote.ExploreSort
import data.remote.PaginatedResult
import data.remote.json.ApiJson
import data.remote.model.ApiPaginatedSuccessEnvelope
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.CloudStickerPack
import data.remote.model.PackCollaborator
import data.remote.model.PublicUserProfile
import data.remote.model.SharePackWithUserRequest
import data.remote.model.UserSearchResult
import data.remote.runApiCall
import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Public user profile/packs, collaborators and user sharing.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5). Typed Result per P3b D6/Family B.
 */
internal class UserDirectoryDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun getPublicUserProfile(userId: String): Result<PublicUserProfile, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Optional) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/users/$userId/public") {
                    authHeader?.let { header(HttpHeaders.Authorization, it) }
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<PublicUserProfile>>(bodyText).data
                ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
        }

    suspend fun getPublicUserPacks(
        userId: String,
        page: Int,
        limit: Int,
        sort: ExploreSort
    ): Result<PaginatedResult<CloudStickerPack>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Optional) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/users/$userId/packs/public") {
                    authHeader?.let { header(HttpHeaders.Authorization, it) }
                    parameter("page", page)
                    parameter("limit", limit)
                    parameter("sort", sort.value)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            val envelope = json.decodeFromString<ApiPaginatedSuccessEnvelope<List<CloudStickerPack>>>(bodyText)
            val pagination = envelope.meta?.pagination
            PaginatedResult(
                data = envelope.data.orEmpty(),
                page = pagination?.page ?: page,
                limit = pagination?.limit ?: limit,
                total = pagination?.total ?: envelope.data.orEmpty().size,
                totalPages = pagination?.totalPages ?: 1
            )
        }

    suspend fun searchUsers(query: String, limit: Int = 10): Result<List<UserSearchResult>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/users/search") {
                    header(HttpHeaders.Authorization, authHeader)
                    parameter("q", query.trim())
                    parameter("limit", limit)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<List<UserSearchResult>>>(bodyText).data.orEmpty()
        }

    suspend fun sharePackWithUser(packId: String, request: SharePackWithUserRequest): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.post("${gateway.baseUrl}/api/v1/sticker-packs/$packId/share") {
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

    suspend fun removePackCollaborator(packId: String, userId: String): EmptyResult<DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.delete("${gateway.baseUrl}/api/v1/sticker-packs/$packId/share") {
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

    suspend fun listPackCollaborators(packId: String): Result<List<PackCollaborator>, DataError.Network> =
        runApiCall(TAG) {
            val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
                gateway.http.get("${gateway.baseUrl}/api/v1/sticker-packs/$packId/collaborators") {
                    header(HttpHeaders.Authorization, authHeader)
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw ApiException(code = AppErrorCode.CloudFetchFailed, message = gateway.extractMessage(bodyText))
            }
            json.decodeFromString<ApiSuccessEnvelope<List<PackCollaborator>>>(bodyText).data.orEmpty()
        }

    private companion object {
        const val TAG = "ExploreUsers"
    }
}
