package data.remote.explore

import data.remote.ApiException
import data.remote.runApiCall
import data.remote.json.ApiJson
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.PackSocialStateData
import data.remote.model.UserFollowStateData
import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/**
 * Pack & user social actions: like/save/download tracking and follow state.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5); façade delegates 1:1.
 * Typed Result per P3b D6/Family B.
 */
internal class PackSocialDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun likePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        runApiCall(TAG) { postSocial("/api/v1/sticker-packs/$packId/like") }

    suspend fun unlikePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        runApiCall(TAG) { deleteSocial("/api/v1/sticker-packs/$packId/like") }

    suspend fun savePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        runApiCall(TAG) { postSocial("/api/v1/sticker-packs/$packId/save") }

    suspend fun unsavePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        runApiCall(TAG) { deleteSocial("/api/v1/sticker-packs/$packId/save") }

    suspend fun trackDownload(packId: String): Result<PackSocialStateData, DataError.Network> =
        runApiCall(TAG) { postSocial("/api/v1/sticker-packs/$packId/download") }

    suspend fun followUser(userId: String): Result<UserFollowStateData, DataError.Network> =
        runApiCall(TAG) { postFollow("/api/v1/users/$userId/follow") }

    suspend fun unfollowUser(userId: String): Result<UserFollowStateData, DataError.Network> =
        runApiCall(TAG) { deleteFollow("/api/v1/users/$userId/follow") }

    private suspend fun postSocial(path: String): PackSocialStateData {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.post("${gateway.baseUrl}$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeSocial(response)
    }

    private suspend fun deleteSocial(path: String): PackSocialStateData {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.delete("${gateway.baseUrl}$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeSocial(response)
    }

    private suspend fun decodeSocial(response: HttpResponse): PackSocialStateData {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = gateway.extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<PackSocialStateData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }

    private suspend fun postFollow(path: String): UserFollowStateData {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.post("${gateway.baseUrl}$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeFollow(response)
    }

    private suspend fun deleteFollow(path: String): UserFollowStateData {
        val response = gateway.authRetry(ExploreAuthMode.Required) { authHeader ->
            gateway.http.delete("${gateway.baseUrl}$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeFollow(response)
    }

    private suspend fun decodeFollow(response: HttpResponse): UserFollowStateData {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = gateway.extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<UserFollowStateData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }

    private companion object {
        const val TAG = "ExploreSocial"
    }
}
