package data.remote

import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import data.remote.model.AcceptSharedPackData
import data.remote.model.AcceptSharedStickerData
import data.remote.model.ApiErrorEnvelope
import data.remote.model.ApiPaginatedSuccessEnvelope
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.BlockedUsersData
import data.remote.model.CloudStickerPack
import data.remote.model.CloudStickerPackShareLink
import data.remote.model.CloudStickerShareLink
import data.remote.model.CloudSticker
import data.remote.model.CreateStickerPackLinkRequest
import data.remote.model.DeleteCountData
import data.remote.model.PackSocialStateData
import data.remote.model.FeaturedTodayData
import data.remote.model.ImportPublicPackResult
import data.remote.model.PackCollaborator
import data.remote.model.ProcessingHistoryItem
import data.remote.model.PromptPresetDto
import domain.model.PromptPreset
import data.remote.model.PublicUserProfile
import data.remote.model.ReportContentRequest
import data.remote.model.SharePackWithUserRequest
import kotlinx.serialization.Serializable
import data.remote.model.UserNotificationItem
import data.remote.model.UserSearchResult
import data.remote.model.SharePreviewPackData
import data.remote.model.SharePreviewStickerData
import data.remote.model.UserFollowStateData
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

enum class ExploreSort(val value: String) {
    RECENT("recent"),
    POPULAR("popular"),
    DOWNLOADS("downloads"),
    LIKES("likes"),
    SAVES("saves")
}

enum class ExploreFeed {
    DISCOVER,
    SAVED,
    FOLLOWING,
    SHARED_WITH_ME
}

data class PaginatedResult<T>(
    val data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

class ExploreApiRepository(
    private val authManager: AuthManager,
    private val authTokenRefresher: AuthTokenRefresher? = null,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
        if (ApiConfig.isDebugLoggingEnabled) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("ExploreApiClient: $message")
                    }
                }
                sanitizeHeader { it == HttpHeaders.Authorization }
                level = LogLevel.ALL
            }
        }
    }

    private fun extractMessage(bodyText: String): String? {
        return runCatching { json.decodeFromString<ApiErrorEnvelope>(bodyText) }
            .getOrNull()
            ?.error
            ?.message
    }

    private suspend fun resolveAccessToken(): String? {
        authManager.getValidAccessToken()?.let { return it }
        return authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = false)
            ?: authManager.getAccessToken()
    }

    private suspend fun forceRefreshAccessToken(): String? {
        val refreshed = authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = true)
        return if (refreshed.isNullOrBlank()) null else refreshed
    }

    private suspend fun withOptionalAuthRetry(request: suspend (String?) -> HttpResponse): HttpResponse {
        val firstToken = resolveAccessToken()
        val firstResponse = request(firstToken?.let { "Bearer $it" })
        if (firstResponse.status != HttpStatusCode.Unauthorized || firstToken.isNullOrBlank()) {
            return firstResponse
        }

        val refreshedToken = forceRefreshAccessToken() ?: return firstResponse
        return request("Bearer $refreshedToken")
    }

    private suspend fun withRequiredAuthRetry(request: suspend (String) -> HttpResponse): HttpResponse {
        val firstToken = resolveAccessToken() ?: throw ApiException(code = AppErrorCode.AuthNotAuthenticated)
        val firstResponse = request("Bearer $firstToken")
        if (firstResponse.status != HttpStatusCode.Unauthorized) return firstResponse
        val refreshedToken = forceRefreshAccessToken() ?: return firstResponse
        return request("Bearer $refreshedToken")
    }

    suspend fun getPublicPacks(
        page: Int,
        limit: Int,
        sort: ExploreSort,
        q: String? = null,
        feed: ExploreFeed = ExploreFeed.DISCOVER
    ): PaginatedResult<CloudStickerPack> {
        val path = when (feed) {
            ExploreFeed.DISCOVER -> "/api/v1/sticker-packs/public"
            ExploreFeed.SAVED -> "/api/v1/sticker-packs/saved"
            ExploreFeed.FOLLOWING -> "/api/v1/sticker-packs/following"
            ExploreFeed.SHARED_WITH_ME -> "/api/v1/sticker-packs/shared-with-me"
        }
        val response = if (feed == ExploreFeed.DISCOVER) {
            withOptionalAuthRetry { authHeader ->
                client.get("$baseUrl$path") {
                    authHeader?.let { header(HttpHeaders.Authorization, it) }
                    parameter("page", page)
                    parameter("limit", limit)
                    parameter("sort", sort.value)
                    if (!q.isNullOrBlank()) parameter("q", q.trim())
                }
            }
        } else {
            withRequiredAuthRetry { authHeader ->
                client.get("$baseUrl$path") {
                    header(HttpHeaders.Authorization, authHeader)
                    parameter("page", page)
                    parameter("limit", limit)
                    parameter("sort", sort.value)
                    if (!q.isNullOrBlank()) parameter("q", q.trim())
                }
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        val envelope = json.decodeFromString<ApiPaginatedSuccessEnvelope<List<CloudStickerPack>>>(bodyText)
        val pagination = envelope.meta?.pagination
        return PaginatedResult(
            data = envelope.data.orEmpty(),
            page = pagination?.page ?: page,
            limit = pagination?.limit ?: limit,
            total = pagination?.total ?: envelope.data.orEmpty().size,
            totalPages = pagination?.totalPages ?: 1
        )
    }

    suspend fun getPublicPackDetail(packId: String): CloudStickerPack {
        val response = withOptionalAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/sticker-packs/public/$packId") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<CloudStickerPack>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
    }

    suspend fun importPublicPack(packId: String): ImportPublicPackResult {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/sticker-packs/$packId/import") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<ImportPublicPackResult>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun getFeaturedToday(): FeaturedTodayData? {
        val response = withOptionalAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/featured/today") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<FeaturedTodayData?>>(bodyText).data
    }

    suspend fun getPublicUserProfile(userId: String): PublicUserProfile {
        val response = withOptionalAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/users/$userId/public") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<PublicUserProfile>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
    }

    suspend fun getPublicUserPacks(
        userId: String,
        page: Int,
        limit: Int,
        sort: ExploreSort
    ): PaginatedResult<CloudStickerPack> {
        val response = withOptionalAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/users/$userId/packs/public") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
                parameter("page", page)
                parameter("limit", limit)
                parameter("sort", sort.value)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        val envelope = json.decodeFromString<ApiPaginatedSuccessEnvelope<List<CloudStickerPack>>>(bodyText)
        val pagination = envelope.meta?.pagination
        return PaginatedResult(
            data = envelope.data.orEmpty(),
            page = pagination?.page ?: page,
            limit = pagination?.limit ?: limit,
            total = pagination?.total ?: envelope.data.orEmpty().size,
            totalPages = pagination?.totalPages ?: 1
        )
    }

    suspend fun searchUsers(query: String, limit: Int = 10): List<UserSearchResult> {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/users/search") {
                header(HttpHeaders.Authorization, authHeader)
                parameter("q", query.trim())
                parameter("limit", limit)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<UserSearchResult>>>(bodyText).data.orEmpty()
    }

    suspend fun sharePackWithUser(packId: String, request: SharePackWithUserRequest) {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/sticker-packs/$packId/share") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
    }

    suspend fun removePackCollaborator(packId: String, userId: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl/api/v1/sticker-packs/$packId/share") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(RemovePackShareBody(userId))
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = extractMessage(bodyText))
        }
    }

    suspend fun listPackCollaborators(packId: String): List<PackCollaborator> {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/sticker-packs/$packId/collaborators") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<PackCollaborator>>>(bodyText).data.orEmpty()
    }

    suspend fun getNotifications(page: Int, limit: Int, unreadOnly: Boolean = false): Pair<List<UserNotificationItem>, Int> {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/notifications") {
                header(HttpHeaders.Authorization, authHeader)
                parameter("page", page)
                parameter("limit", limit)
                if (unreadOnly) parameter("unreadOnly", true)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        val envelope = json.decodeFromString<ApiPaginatedSuccessEnvelope<List<UserNotificationItem>>>(bodyText)
        return envelope.data.orEmpty() to (envelope.meta?.unreadCount ?: 0)
    }

    suspend fun markNotificationRead(id: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.patch("$baseUrl/api/v1/notifications/$id/read") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = extractMessage(response.bodyAsText()))
        }
    }

    suspend fun markAllNotificationsRead() {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/notifications/read-all") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = extractMessage(response.bodyAsText()))
        }
    }

    suspend fun getPromptPresets(category: String? = null): List<PromptPreset> {
        val response = client.get("$baseUrl/api/v1/prompt-presets") {
            category?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("category", it) }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<PromptPresetDto>>>(bodyText)
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
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeSocial(response)
    }

    private suspend fun deleteSocial(path: String): PackSocialStateData {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeSocial(response)
    }

    private suspend fun decodeSocial(response: HttpResponse): PackSocialStateData {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<PackSocialStateData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }

    private suspend fun postFollow(path: String): UserFollowStateData {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeFollow(response)
    }

    private suspend fun deleteFollow(path: String): UserFollowStateData {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl$path") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        return decodeFollow(response)
    }

    private suspend fun decodeFollow(response: HttpResponse): UserFollowStateData {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<UserFollowStateData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }

    suspend fun previewPackShare(token: String): SharePreviewPackData {
        val response = client.get("$baseUrl/api/v1/share/pack/$token")
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<SharePreviewPackData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
    }

    suspend fun previewStickerShare(token: String): SharePreviewStickerData {
        val response = client.get("$baseUrl/api/v1/share/sticker/$token")
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<SharePreviewStickerData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
    }

    suspend fun acceptPackShare(token: String): AcceptSharedPackData {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/share/pack/$token/accept") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<AcceptSharedPackData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun acceptStickerShare(token: String): AcceptSharedStickerData {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/share/sticker/$token/accept") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<AcceptSharedStickerData>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun getPackLinks(packId: String): List<CloudStickerPackShareLink> {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/sticker-packs/$packId/links") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<CloudStickerPackShareLink>>>(bodyText).data.orEmpty()
    }

    suspend fun createPackLink(packId: String, request: CreateStickerPackLinkRequest): CloudStickerPackShareLink {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/sticker-packs/$packId/link") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<CloudStickerPackShareLink>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun revokePackLink(packId: String, linkId: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl/api/v1/sticker-packs/$packId/link/$linkId") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = extractMessage(bodyText))
        }
    }

    suspend fun getProcessingHistory(type: String? = null): List<ProcessingHistoryItem> {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/processing-history") {
                header(HttpHeaders.Authorization, authHeader)
                if (!type.isNullOrBlank()) parameter("type", type)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<ProcessingHistoryItem>>>(bodyText).data.orEmpty()
    }

    suspend fun deleteHistoryItem(id: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl/api/v1/processing-history/$id") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = extractMessage(bodyText))
        }
    }

    suspend fun clearHistory(type: String? = null): Int {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl/api/v1/processing-history") {
                header(HttpHeaders.Authorization, authHeader)
                if (!type.isNullOrBlank()) parameter("type", type)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<DeleteCountData>>(bodyText).data?.deletedCount ?: 0
    }

    suspend fun reportPack(packId: String, reason: String, details: String? = null) {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/sticker-packs/$packId/report") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(ReportContentRequest(reason = reason, details = details))
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(response.bodyAsText()))
        }
    }

    suspend fun reportProcessingHistory(historyId: String, reason: String, details: String? = null) {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/processing-history/$historyId/report") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(ReportContentRequest(reason = reason, details = details))
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(response.bodyAsText()))
        }
    }

    suspend fun blockUser(userId: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/users/$userId/block") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(response.bodyAsText()))
        }
    }

    suspend fun listBlockedUsers(): BlockedUsersData {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/users/blocked") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<BlockedUsersData>>(bodyText).data
            ?: BlockedUsersData()
    }

    suspend fun unblockUser(userId: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl/api/v1/users/$userId/block") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = extractMessage(response.bodyAsText()))
        }
    }

    suspend fun getStickerLinks(stickerId: String): List<CloudStickerShareLink> {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/stickers/$stickerId/links") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<CloudStickerShareLink>>>(bodyText).data.orEmpty()
    }

    suspend fun createStickerLink(stickerId: String, request: CreateStickerPackLinkRequest): CloudStickerShareLink {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/stickers/$stickerId/link") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<CloudStickerShareLink>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun revokeStickerLink(stickerId: String, linkId: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl/api/v1/stickers/$stickerId/link/$linkId") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = extractMessage(bodyText))
        }
    }

    suspend fun shareStickerWithUser(stickerId: String, request: SharePackWithUserRequest) {
        val response = withRequiredAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/stickers/$stickerId/share") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
    }

    suspend fun removeStickerShare(stickerId: String, userId: String) {
        val response = withRequiredAuthRetry { authHeader ->
            client.delete("$baseUrl/api/v1/stickers/$stickerId/share") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(RemovePackShareBody(userId))
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed, message = extractMessage(bodyText))
        }
    }

    suspend fun listStickerCollaborators(stickerId: String): List<data.remote.model.StickerCollaborator> {
        val response = withRequiredAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/stickers/$stickerId/collaborators") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        return json.decodeFromString<ApiSuccessEnvelope<List<data.remote.model.StickerCollaborator>>>(bodyText)
            .data.orEmpty()
    }
}

@Serializable
private data class RemovePackShareBody(val userId: String)
