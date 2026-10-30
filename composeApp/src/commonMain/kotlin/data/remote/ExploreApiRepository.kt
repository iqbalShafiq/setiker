package data.remote

import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import data.remote.explore.ExploreAuthMode
import data.remote.explore.ExploreGateway
import data.remote.explore.HistoryDataSource
import data.remote.explore.ModerationDataSource
import data.remote.explore.NotificationDataSource
import data.remote.explore.PackShareLinkDataSource
import data.remote.explore.PackSocialDataSource
import data.remote.explore.StickerShareLinkDataSource
import data.remote.explore.UserDirectoryDataSource
import data.remote.json.ApiJson
import data.remote.model.AcceptSharedPackData
import data.remote.model.AcceptSharedStickerData
import data.remote.model.ApiErrorEnvelope
import data.remote.model.ApiPaginatedSuccessEnvelope
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.BlockedUsersData
import data.remote.model.CloudStickerPack
import data.remote.model.CloudStickerPackShareLink
import data.remote.model.CloudStickerShareLink
import data.remote.model.CreateStickerPackLinkRequest
import data.remote.model.FeaturedTodayData
import data.remote.model.ImportPublicPackResult
import data.remote.model.PackCollaborator
import data.remote.model.PackSocialStateData
import data.remote.model.ProcessingHistoryItem
import data.remote.model.PublicUserProfile
import data.remote.model.SharePackWithUserRequest
import data.remote.model.SharePreviewPackData
import data.remote.model.SharePreviewStickerData
import data.remote.model.StickerCollaborator
import data.remote.model.UserFollowStateData
import data.remote.model.UserNotificationItem
import data.remote.model.UserSearchResult
import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.EmptyResult
import domain.error.Result
import domain.model.PromptPreset
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

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

/**
 * Public API facade of the explore plane (spec P3b, D5). Endpoint bodies live in the
 * `data.remote.explore` data sources, wired through a private [ExploreGateway]; this class
 * keeps the home surface (public packs/detail, import, featured), the client, and the
 * auth-retry implementation. Method signatures are unchanged, so call sites stay untouched.
 */
class ExploreApiRepository(
    private val authManager: AuthManager,
    private val authTokenRefresher: AuthTokenRefresher? = null,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = ApiJson.codec

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

    private val gateway: ExploreGateway = object : ExploreGateway {
        override val http: HttpClient get() = client
        override val baseUrl: String get() = this@ExploreApiRepository.baseUrl

        override suspend fun authRetry(
            mode: ExploreAuthMode,
            request: suspend (authHeader: String?) -> HttpResponse,
        ): HttpResponse = runAuthRetry(mode, request)

        override fun extractMessage(bodyText: String): String? =
            this@ExploreApiRepository.extractMessage(bodyText)
    }

    // Must be declared after `gateway` so the sources capture the finished object.
    private val social = PackSocialDataSource(gateway)
    private val packShares = PackShareLinkDataSource(gateway)
    private val stickerShares = StickerShareLinkDataSource(gateway)
    private val userDirectory = UserDirectoryDataSource(gateway)
    private val notifications = NotificationDataSource(gateway)
    private val history = HistoryDataSource(gateway)
    private val moderation = ModerationDataSource(gateway)

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

    /**
     * Single implementation of the optional/required auth-retry pair: `Required` throws when no
     * token is available, `Optional` proceeds unauthenticated; both retry once on 401 after a
     * forced token refresh. `Public` never attaches an Authorization header.
     */
    private suspend fun runAuthRetry(
        mode: ExploreAuthMode,
        request: suspend (String?) -> HttpResponse,
    ): HttpResponse = when (mode) {
        ExploreAuthMode.Public -> {
            request(null)
        }
        ExploreAuthMode.Optional -> {
            val firstToken = resolveAccessToken()
            val firstResponse = request(firstToken?.let { "Bearer $it" })
            if (firstResponse.status != HttpStatusCode.Unauthorized || firstToken.isNullOrBlank()) {
                firstResponse
            } else {
                val refreshedToken = forceRefreshAccessToken() ?: return firstResponse
                request("Bearer $refreshedToken")
            }
        }
        ExploreAuthMode.Required -> {
            val firstToken = resolveAccessToken() ?: throw ApiException(code = AppErrorCode.AuthNotAuthenticated)
            val firstResponse = request("Bearer $firstToken")
            if (firstResponse.status != HttpStatusCode.Unauthorized) return firstResponse
            val refreshedToken = forceRefreshAccessToken() ?: return firstResponse
            request("Bearer $refreshedToken")
        }
    }

    suspend fun getPublicPacks(
        page: Int,
        limit: Int,
        sort: ExploreSort,
        q: String? = null,
        feed: ExploreFeed = ExploreFeed.DISCOVER
    ): Result<PaginatedResult<CloudStickerPack>, DataError.Network> =
        runApiCall("ExploreApi") {
        val path = when (feed) {
            ExploreFeed.DISCOVER -> "/api/v1/sticker-packs/public"
            ExploreFeed.SAVED -> "/api/v1/sticker-packs/saved"
            ExploreFeed.FOLLOWING -> "/api/v1/sticker-packs/following"
            ExploreFeed.SHARED_WITH_ME -> "/api/v1/sticker-packs/shared-with-me"
        }
        val response = if (feed == ExploreFeed.DISCOVER) {
            runAuthRetry(ExploreAuthMode.Optional) { authHeader ->
                client.get("$baseUrl$path") {
                    authHeader?.let { header(HttpHeaders.Authorization, it) }
                    parameter("page", page)
                    parameter("limit", limit)
                    parameter("sort", sort.value)
                    if (!q.isNullOrBlank()) parameter("q", q.trim())
                }
            }
        } else {
            runAuthRetry(ExploreAuthMode.Required) { authHeader ->
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
        PaginatedResult(
            data = envelope.data.orEmpty(),
            page = pagination?.page ?: page,
            limit = pagination?.limit ?: limit,
            total = pagination?.total ?: envelope.data.orEmpty().size,
            totalPages = pagination?.totalPages ?: 1
        )
        }

    suspend fun getPublicPackDetail(packId: String): Result<CloudStickerPack, DataError.Network> =
        runApiCall("ExploreApi") {
        val response = runAuthRetry(ExploreAuthMode.Optional) { authHeader ->
            client.get("$baseUrl/api/v1/sticker-packs/public/$packId") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        json.decodeFromString<ApiSuccessEnvelope<CloudStickerPack>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudFetchFailed)
        }

    suspend fun importPublicPack(packId: String): Result<ImportPublicPackResult, DataError.Network> =
        runApiCall("ExploreApi") {
        val response = runAuthRetry(ExploreAuthMode.Required) { authHeader ->
            client.post("$baseUrl/api/v1/sticker-packs/$packId/import") {
                header(HttpHeaders.Authorization, authHeader)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed, message = extractMessage(bodyText))
        }
        json.decodeFromString<ApiSuccessEnvelope<ImportPublicPackResult>>(bodyText).data
            ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }

    suspend fun getFeaturedToday(): Result<FeaturedTodayData?, DataError.Network> =
        runApiCall("ExploreApi") {
        val response = runAuthRetry(ExploreAuthMode.Optional) { authHeader ->
            client.get("$baseUrl/api/v1/featured/today") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed, message = extractMessage(bodyText))
        }
        json.decodeFromString<ApiSuccessEnvelope<FeaturedTodayData?>>(bodyText).data
        }

    suspend fun getPublicUserProfile(userId: String): Result<PublicUserProfile, DataError.Network> =
        userDirectory.getPublicUserProfile(userId)

    suspend fun getPublicUserPacks(
        userId: String,
        page: Int,
        limit: Int,
        sort: ExploreSort
    ): Result<PaginatedResult<CloudStickerPack>, DataError.Network> =
        userDirectory.getPublicUserPacks(userId, page, limit, sort)

    suspend fun searchUsers(query: String, limit: Int = 10): Result<List<UserSearchResult>, DataError.Network> =
        userDirectory.searchUsers(query, limit)

    suspend fun sharePackWithUser(packId: String, request: SharePackWithUserRequest): EmptyResult<DataError.Network> =
        userDirectory.sharePackWithUser(packId, request)

    suspend fun removePackCollaborator(packId: String, userId: String): EmptyResult<DataError.Network> =
        userDirectory.removePackCollaborator(packId, userId)

    suspend fun listPackCollaborators(packId: String): Result<List<PackCollaborator>, DataError.Network> =
        userDirectory.listPackCollaborators(packId)

    suspend fun getNotifications(page: Int, limit: Int, unreadOnly: Boolean = false): Result<Pair<List<UserNotificationItem>, Int>, DataError.Network> =
        notifications.getNotifications(page, limit, unreadOnly)

    suspend fun markNotificationRead(id: String): EmptyResult<DataError.Network> =
        notifications.markNotificationRead(id)

    suspend fun markAllNotificationsRead(): EmptyResult<DataError.Network> =
        notifications.markAllNotificationsRead()

    suspend fun getPromptPresets(category: String? = null): Result<List<PromptPreset>, DataError.Network> =
        history.getPromptPresets(category)

    suspend fun likePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        social.likePack(packId)

    suspend fun unlikePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        social.unlikePack(packId)

    suspend fun savePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        social.savePack(packId)

    suspend fun unsavePack(packId: String): Result<PackSocialStateData, DataError.Network> =
        social.unsavePack(packId)

    suspend fun trackDownload(packId: String): Result<PackSocialStateData, DataError.Network> =
        social.trackDownload(packId)

    suspend fun followUser(userId: String): Result<UserFollowStateData, DataError.Network> =
        social.followUser(userId)

    suspend fun unfollowUser(userId: String): Result<UserFollowStateData, DataError.Network> =
        social.unfollowUser(userId)

    suspend fun previewPackShare(token: String): Result<SharePreviewPackData, DataError.Network> =
        packShares.previewPackShare(token)

    suspend fun previewStickerShare(token: String): Result<SharePreviewStickerData, DataError.Network> =
        packShares.previewStickerShare(token)

    suspend fun acceptPackShare(token: String): Result<AcceptSharedPackData, DataError.Network> =
        packShares.acceptPackShare(token)

    suspend fun acceptStickerShare(token: String): Result<AcceptSharedStickerData, DataError.Network> =
        packShares.acceptStickerShare(token)

    suspend fun getPackLinks(packId: String): Result<List<CloudStickerPackShareLink>, DataError.Network> =
        packShares.getPackLinks(packId)

    suspend fun createPackLink(packId: String, request: CreateStickerPackLinkRequest): Result<CloudStickerPackShareLink, DataError.Network> =
        packShares.createPackLink(packId, request)

    suspend fun revokePackLink(packId: String, linkId: String): EmptyResult<DataError.Network> =
        packShares.revokePackLink(packId, linkId)

    suspend fun getStickerLinks(stickerId: String): Result<List<CloudStickerShareLink>, DataError.Network> =
        stickerShares.getStickerLinks(stickerId)

    suspend fun createStickerLink(stickerId: String, request: CreateStickerPackLinkRequest): Result<CloudStickerShareLink, DataError.Network> =
        stickerShares.createStickerLink(stickerId, request)

    suspend fun revokeStickerLink(stickerId: String, linkId: String): EmptyResult<DataError.Network> =
        stickerShares.revokeStickerLink(stickerId, linkId)

    suspend fun shareStickerWithUser(stickerId: String, request: SharePackWithUserRequest): EmptyResult<DataError.Network> =
        stickerShares.shareStickerWithUser(stickerId, request)

    suspend fun removeStickerShare(stickerId: String, userId: String): EmptyResult<DataError.Network> =
        stickerShares.removeStickerShare(stickerId, userId)

    suspend fun listStickerCollaborators(stickerId: String): Result<List<StickerCollaborator>, DataError.Network> =
        stickerShares.listStickerCollaborators(stickerId)

    suspend fun getProcessingHistory(type: String? = null): Result<List<ProcessingHistoryItem>, DataError.Network> =
        history.getProcessingHistory(type)

    suspend fun deleteHistoryItem(id: String): EmptyResult<DataError.Network> =
        history.deleteHistoryItem(id)

    suspend fun clearHistory(type: String? = null): Result<Int, DataError.Network> =
        history.clearHistory(type)

    suspend fun reportPack(packId: String, reason: String, details: String? = null): EmptyResult<DataError.Network> =
        moderation.reportPack(packId, reason, details)

    suspend fun reportProcessingHistory(historyId: String, reason: String, details: String? = null): EmptyResult<DataError.Network> =
        moderation.reportProcessingHistory(historyId, reason, details)

    suspend fun blockUser(userId: String): EmptyResult<DataError.Network> =
        moderation.blockUser(userId)

    suspend fun listBlockedUsers(): Result<BlockedUsersData, DataError.Network> =
        moderation.listBlockedUsers()

    suspend fun unblockUser(userId: String): EmptyResult<DataError.Network> =
        moderation.unblockUser(userId)
}
