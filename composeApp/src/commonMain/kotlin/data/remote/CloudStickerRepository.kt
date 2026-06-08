package data.remote

import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.CloudStickerPack
import data.remote.model.CreateStickerPackRequest
import data.remote.model.SyncData
import data.remote.model.UpdateStickerPackRequest
import data.remote.model.UploadData
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import data.sync.encodeLastSyncAt
import data.sync.normalizePackVisibilityForApi
import data.remote.readFileBytes

class CloudStickerRepository(
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
                        println("CloudSyncClient: $message")
                    }
                }
                sanitizeHeader { it == HttpHeaders.Authorization }
                level = LogLevel.ALL
            }
        }
    }

    private suspend fun resolveAccessToken(): String? {
        authManager.getValidAccessToken()?.let { return it }
        return authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = false)
            ?: authManager.getAccessToken()
    }

    private suspend fun forceRefreshAccessToken(): String? {
        val refreshed = authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = true)
        if (refreshed.isNullOrBlank()) {
            return null
        }
        return refreshed
    }

    private suspend fun withAuthRetry(request: suspend (String) -> HttpResponse): HttpResponse {
        val firstToken = resolveAccessToken() ?: throw ApiException(code = AppErrorCode.AuthNotAuthenticated)
        val firstResponse = request("Bearer $firstToken")
        if (firstResponse.status != HttpStatusCode.Unauthorized) {
            return firstResponse
        }

        val refreshedToken = forceRefreshAccessToken() ?: return firstResponse
        return request("Bearer $refreshedToken")
    }

    suspend fun updatePack(cloudPackId: String, request: UpdateStickerPackRequest): CloudStickerPack {
        val response = withAuthRetry { authHeader ->
            client.put("$baseUrl/api/v1/sticker-packs/$cloudPackId") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed)
        }
        val envelope = json.decodeFromString<ApiSuccessEnvelope<CloudStickerPack>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }

    suspend fun uploadPack(request: CreateStickerPackRequest, stickerPackId: String? = null): UploadData {
        val response = withAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/upload") {
                header(HttpHeaders.Authorization, authHeader)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            if (stickerPackId.isNullOrBlank()) {
                                append("stickerPackName", request.name)
                                request.description?.let { append("stickerPackDescription", it) }
                            } else {
                                append("stickerPackId", stickerPackId)
                            }
                            append("visibility", normalizePackVisibilityForApi(request.visibility))
                            request.stickers.sortedBy { it.order }.forEachIndexed { index, sticker ->
                                append(
                                    key = "images",
                                    value = readFileBytes(sticker.url),
                                    headers = Headers.build {
                                        append(HttpHeaders.ContentType, ContentType.Image.Any.toString())
                                        append(HttpHeaders.ContentDisposition, "filename=\"${sticker.filename.ifBlank { "sticker-$index.webp" }}\"")
                                    },
                                )
                            }
                        }
                    )
                )
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }
        val envelope = json.decodeFromString<ApiSuccessEnvelope<UploadData>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun deletePackViaUpload(stickerPackId: String) {
        val response = withAuthRetry { authHeader ->
            client.post("$baseUrl/api/v1/upload") {
                header(HttpHeaders.Authorization, authHeader)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("action", "delete")
                            append("stickerPackId", stickerPackId)
                        }
                    )
                )
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed)
        }
    }

    suspend fun sync(lastSyncAt: Long?): SyncData {
        val response = withAuthRetry { authHeader ->
            client.get("$baseUrl/api/v1/sync") {
                header(HttpHeaders.Authorization, authHeader)
                if (lastSyncAt != null) {
                    parameter("lastSyncAt", encodeLastSyncAt(lastSyncAt))
                }
            }
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudSyncFailed)
        }
        val envelope = json.decodeFromString<ApiSuccessEnvelope<SyncData>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudSyncFailed)
    }

    suspend fun downloadBytes(url: String): ByteArray {
        val response = client.get(normalizeDownloadUrl(url))
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed)
        }
        return response.body()
    }

    private fun normalizeDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return trimmed

        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
            return baseUrl.trimEnd('/') + path
        }

        val sourceUrl = Url(trimmed)
        val sourceHost = sourceUrl.host.lowercase()
        val isLocalHost = sourceHost == "localhost" || sourceHost == "127.0.0.1" || sourceHost == "::1"
        if (!isLocalHost) return trimmed

        val apiBase = Url(baseUrl)
        return URLBuilder(trimmed).apply {
            protocol = apiBase.protocol
            host = apiBase.host
            port = apiBase.port
        }.buildString()
    }
}
