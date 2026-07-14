package data.auth

import data.auth.model.AuthResponse
import data.auth.model.ChangePasswordRequest
import data.auth.model.DeleteAccountRequest
import data.auth.model.GoogleIdTokenRequest
import data.auth.model.LinkGoogleWithPasswordRequest
import data.auth.model.LoginRequest
import data.auth.model.RefreshTokenRequest
import data.auth.model.RegisterRequest
import data.auth.model.SetPasswordRequest
import data.auth.model.UpdateProfileRequest
import data.auth.model.UserProfileResponse
import data.remote.ApiConfig
import data.remote.ApiErrorParser
import data.remote.ApiException
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
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
            message = parsed?.message ?: bodyText,
            subcode = parsed?.subcode
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

    suspend fun loginWithGoogle(idToken: String): AuthResponse {
        val response = client.post("$baseUrl/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleIdTokenRequest(idToken = idToken))
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthGoogleFailed)
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun linkGoogleWithPassword(idToken: String, email: String, password: String): AuthResponse {
        val response = client.post("$baseUrl/api/v1/auth/google/link-with-password") {
            contentType(ContentType.Application.Json)
            setBody(
                LinkGoogleWithPasswordRequest(
                    idToken = idToken,
                    email = email,
                    password = password
                )
            )
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthGoogleFailed)
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun linkGoogle(token: String, idToken: String): UserProfileResponse {
        val response = client.post("$baseUrl/api/v1/auth/google/link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(GoogleIdTokenRequest(idToken = idToken))
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthGoogleFailed)
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun unlinkGoogle(token: String): UserProfileResponse {
        val response = client.delete("$baseUrl/api/v1/auth/google") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthGoogleFailed)
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun setPassword(token: String, newPassword: String) {
        val response = client.post("$baseUrl/api/v1/auth/set-password") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(SetPasswordRequest(newPassword = newPassword))
        }
        if (!response.status.isSuccess()) {
            val bodyText = response.bodyAsText()
            throwApiError(bodyText, AppErrorCode.AuthChangePasswordFailed)
        }
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

    suspend fun logout(accessToken: String? = null, refreshToken: String? = null) {
        val response = client.post("$baseUrl/api/v1/auth/logout") {
            contentType(ContentType.Application.Json)
            accessToken?.takeIf { it.isNotBlank() }?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
            val token = refreshToken ?: cookieStorage.refreshToken
            if (!token.isNullOrBlank()) {
                setBody(RefreshTokenRequest(refreshToken = token))
            }
        }
        if (!response.status.isSuccess()) {
            // Best-effort logout; client still clears local session.
        }
        cookieStorage.clear()
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

    suspend fun updateProfile(token: String, request: UpdateProfileRequest): UserProfileResponse {
        val response = client.put("$baseUrl/api/v1/auth/me") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throwApiError(bodyText, AppErrorCode.AuthUpdateProfileFailed)
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
