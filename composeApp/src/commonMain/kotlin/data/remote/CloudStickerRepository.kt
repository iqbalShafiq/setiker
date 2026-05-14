package data.remote

import data.auth.AuthManager
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.CloudStickerPack
import data.remote.model.CreateStickerPackRequest
import data.remote.model.SyncData
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
import kotlinx.serialization.json.Json

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
        val envelope = json.decodeFromString<ApiSuccessEnvelope<SyncData>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudSyncFailed)
    }
}
