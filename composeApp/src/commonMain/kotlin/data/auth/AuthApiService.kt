package data.auth

import data.auth.model.AuthResponse
import data.auth.model.ChangePasswordRequest
import data.auth.model.LoginRequest
import data.auth.model.RegisterRequest
import data.auth.model.UserProfileResponse
import data.auth.model.DeleteAccountRequest
import data.auth.model.RefreshTokenRequest
import data.remote.ApiConfig
import data.remote.ApiErrorParser
import data.remote.ApiException
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AuthApiService(
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val cookieStorage = RefreshTokenStorage()

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpCookies) {
            storage = cookieStorage
        }
    }

    fun getRefreshToken(): String? = cookieStorage.refreshToken

    fun clearRefreshToken() { cookieStorage.clear() }

    private fun throwApiError(bodyText: String, fallback: AppErrorCode): Nothing {
        val parsed = ApiErrorParser.parse(bodyText)
        throw ApiException(
            code = parsed?.code ?: fallback,
            message = parsed?.message ?: bodyText
        )
    }

    suspend fun register(request: RegisterRequest): AuthResponse {
        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthRegisterFailed)
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val response = client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthLoginFailed)
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun refreshToken(storedRefreshToken: String? = null): AuthResponse {
        if (cookieStorage.refreshToken.isNullOrBlank() && !storedRefreshToken.isNullOrBlank()) {
            cookieStorage.setRefreshToken(storedRefreshToken)
        }
        val response = client.post("$baseUrl/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            storedRefreshToken?.takeIf { it.isNotBlank() }?.let { token ->
                setBody(RefreshTokenRequest(refreshToken = token))
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthRefreshFailed)
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun logout() {
        client.post("$baseUrl/api/v1/auth/logout")
    }

    suspend fun getProfile(token: String): UserProfileResponse {
        val response = client.get("$baseUrl/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.AuthProfileFailed, message = "Failed to get profile")
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun changePassword(token: String, request: ChangePasswordRequest) {
        val response = client.post("$baseUrl/api/v1/auth/change-password") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val bodyText = response.bodyAsText()
            throwApiError(bodyText, AppErrorCode.AuthChangePasswordFailed)
        }
    }

    suspend fun deleteAccount(token: String, currentPassword: String? = null) {
        val response = client.delete("$baseUrl/api/v1/auth/me") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            if (!currentPassword.isNullOrBlank()) {
                setBody(DeleteAccountRequest(currentPassword = currentPassword))
            }
        }
        if (!response.status.isSuccess()) {
            val bodyText = response.bodyAsText()
            throwApiError(bodyText, AppErrorCode.CloudDeleteFailed)
        }
    }
}

private class RefreshTokenStorage : CookiesStorage {
    var refreshToken: String? = null
        private set

    override suspend fun addCookie(requestUrl: io.ktor.http.Url, cookie: Cookie) {
        if (cookie.name == "refresh_token") {
            refreshToken = cookie.value
        }
    }

    override fun close() {}

    override suspend fun get(requestUrl: io.ktor.http.Url): List<Cookie> {
        return refreshToken?.let {
            listOf(Cookie(name = "refresh_token", value = it))
        } ?: emptyList()
    }

    fun clear() {
        refreshToken = null
    }

    fun setRefreshToken(token: String) {
        refreshToken = token
    }
}
