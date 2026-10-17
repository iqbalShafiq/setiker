package data.remote.explore

import data.remote.json.ApiJson
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.PackSocialStateData
import data.remote.model.UserFollowStateData
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import domain.error.AppErrorCode
import data.remote.ApiException

/**
 * Pack & user social actions: like/save/download tracking and follow state.
 * Moved verbatim from `ExploreApiRepository` (spec P3b, D5); façade delegates 1:1.
 */
internal class PackSocialDataSource(
    private val gateway: ExploreGateway,
) {
    private val json = ApiJson.codec

    suspend fun likePack(packId: String): PackSocialStateData = postSocial("/api/v1/sticker-packs/$packId/like")
    suspend fun unlikePack(packId: String): PackSocialStateData = deleteSocial("/api/v1/sticker-packs/$packId/like")
    suspend fun savePack(packId: String): PackSocialStateData = postSocial("/api/v1/sticker-packs/$packId/save")
    suspend fun unsavePack(packId: String): PackSocialStateData = deleteSocial("/api/v1/sticker-packs/$packId/save")
    suspend fun trackDownload(packId: String): PackSocialStateData = postSocial("/api/v1/sticker-packs/$packId/download")

    suspend fun followUser(userId: String): UserFollowStateData {
        return postFollow("/api/v1/users/$userId/follow")
    }

    suspend fun unfollowUser(userId: String): UserFollowStateData {
        return deleteFollow("/api/v1/users/$userId/follow")
    }

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

    private suspend fun decodeSocial(response: io.ktor.client.statement.HttpResponse): PackSocialStateData {
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

    private suspend fun decodeFollow(response: io.ktor.client.statement.HttpResponse): UserFollowStateData {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = gateway.extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<UserFollowStateData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }
}
