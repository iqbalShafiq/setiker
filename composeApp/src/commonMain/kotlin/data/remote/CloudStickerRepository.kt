package data.remote

import data.auth.AuthManager
import data.remote.model.ApiSuccessEnvelope
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CreateStickerPackRequest(
    val name: String,
    val description: String? = null,
    val visibility: String = "PRIVATE",
    val stickers: List<StickerPackStickerInput> = emptyList()
)

@Serializable
data class StickerPackStickerInput(
    val name: String,
    val filename: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val order: Int = 0
)

@Serializable
data class CloudStickerPack(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val visibility: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class SyncData(
    val stickerPacks: SyncDelta? = null,
    val stickers: SyncDelta? = null,
    val syncToken: String? = null
)

@Serializable
data class SyncDelta(
    val created: List<CloudStickerPack> = emptyList(),
    val updated: List<CloudStickerPack> = emptyList(),
    val deleted: List<CloudStickerPack> = emptyList()
)

@Serializable
data class SyncResponseData(
    val success: Boolean,
    val data: SyncData? = null
)

class CloudStickerRepository(
    private val authManager: AuthManager,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    private suspend fun getAuthHeader(): String {
        val token = authManager.getAccessToken()
            ?: throw ApiException(code = AppErrorCode.AuthNotAuthenticated)
        return "Bearer $token"
    }

    suspend fun getMyPacks(): List<CloudStickerPack> {
        val response = client.get("$baseUrl/api/v1/sticker-packs") {
            header(HttpHeaders.Authorization, getAuthHeader())
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed)
        }
        val envelope = json.decodeFromString<ApiSuccessEnvelope<List<CloudStickerPack>>>(response.bodyAsText())
        return envelope.data ?: emptyList()
    }

    suspend fun createPack(request: CreateStickerPackRequest): CloudStickerPack {
        val response = client.post("$baseUrl/api/v1/sticker-packs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, getAuthHeader())
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }
        val envelope = json.decodeFromString<ApiSuccessEnvelope<CloudStickerPack>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }

    suspend fun updatePack(packId: String, request: CreateStickerPackRequest): CloudStickerPack {
        val response = client.put("$baseUrl/api/v1/sticker-packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, getAuthHeader())
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed)
        }
        val envelope = json.decodeFromString<ApiSuccessEnvelope<CloudStickerPack>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }

    suspend fun deletePack(packId: String) {
        val response = client.delete("$baseUrl/api/v1/sticker-packs/$packId") {
            header(HttpHeaders.Authorization, getAuthHeader())
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed)
        }
    }

    suspend fun sync(lastSyncAt: Long?): SyncData {
        val url = if (lastSyncAt != null) {
            "$baseUrl/api/v1/sync?lastSyncAt=$lastSyncAt"
        } else {
            "$baseUrl/api/v1/sync"
        }
        val response = client.get(url) {
            header(HttpHeaders.Authorization, getAuthHeader())
        }
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudSyncFailed)
        }
        val envelope = json.decodeFromString<SyncResponseData>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudSyncFailed)
    }
}
