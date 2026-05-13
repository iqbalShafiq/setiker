package data.auth

import data.auth.model.AuthResponse
import data.auth.model.ChangePasswordRequest
import data.auth.model.LoginRequest
import data.auth.model.RegisterRequest
import data.auth.model.UserProfileResponse
import data.remote.ApiConfig
import data.remote.ApiException
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AuthApiService(
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun register(request: RegisterRequest): AuthResponse {
        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(
                code = AppErrorCode.AuthRegisterFailed,
                message = bodyText
            )
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
            throw ApiException(
                code = AppErrorCode.AuthLoginFailed,
                message = bodyText
            )
        }
        return json.decodeFromString(bodyText)
    }

    suspend fun refreshToken(): AuthResponse {
        val response = client.post("$baseUrl/api/v1/auth/refresh")
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.AuthRefreshFailed, message = "Token refresh failed")
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
            throw ApiException(code = AppErrorCode.AuthChangePasswordFailed, message = "Password change failed")
        }
    }
}
