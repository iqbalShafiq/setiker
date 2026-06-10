package data.remote

import io.ktor.http.URLBuilder
import io.ktor.http.Url

/**
 * Resolves API-relative asset paths (e.g. `/uploads/...`) to absolute URLs for Coil and downloads.
 */
fun resolveApiUrl(rawUrl: String?, baseUrl: String = ApiConfig.baseUrl): String? {
    if (rawUrl.isNullOrBlank()) return null

    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return null

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
