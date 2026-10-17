package data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import data.remote.json.ApiJson
import util.AppLog

/**
 * Long-running request profile per service class.
 * `LongOperation` preserves the 600 s AI generation/video pipeline timeouts that
 * `SetikerApiService` has always used; `Default` preserves the 60 s sync/explore behavior.
 * A consolidation bug here would clamp AI generation to 60 s — verified by device smoke
 * in this project's P3b plan (Review Focus #1).
 */
enum class ApiTimeoutProfile(val requestMillis: Long) {
    Default(60_000),
    LongOperation(600_000),
}

/**
 * Shared Ktor client factory (spec P3b, D3). Replaces the seven per-file `HttpClient { ... }`
 * blocks whose configs had drifted (AI 600 s vs sync 60 s vs auth-without-timeout) and whose
 * debug logging dumped request/response bodies — including multipart binary uploads — to
 * stdout via `LogLevel.ALL`.
 *
 * The shared client never logs bodies: `LogLevel.INFO` captures method + URL + status only.
 * `Authorization` is sanitized exactly as before.
 */
fun createApiClient(
    tag: String,
    profile: ApiTimeoutProfile = ApiTimeoutProfile.Default,
): HttpClient = HttpClient {
    install(ContentNegotiation) { json(ApiJson.codec) }
    install(HttpTimeout) {
        requestTimeoutMillis = profile.requestMillis
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = profile.requestMillis
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                AppLog.d(tag, message)
            }
        }
        sanitizeHeader { it == HttpHeaders.Authorization }
        level = LogLevel.INFO
    }
}
