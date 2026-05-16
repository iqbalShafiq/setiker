package data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthTokenRefresher(
    private val authManager: AuthManager,
    private val authApiService: AuthApiService
) {
    private val refreshMutex = Mutex()

    suspend fun refreshAccessToken(clearTokensOnFailure: Boolean = true): String? = refreshMutex.withLock {
        val storedRefreshToken = authManager.getRefreshToken()
        if (storedRefreshToken.isNullOrBlank() && authApiService.getRefreshToken().isNullOrBlank()) {
            if (clearTokensOnFailure) authManager.clearTokens()
            return@withLock null
        }

        val response = runCatching { authApiService.refreshToken(storedRefreshToken) }.getOrElse {
            if (clearTokensOnFailure) authManager.clearTokens()
            return@withLock null
        }

        val accessToken = response.data?.accessToken
        if (accessToken.isNullOrBlank()) {
            if (clearTokensOnFailure) authManager.clearTokens()
            return@withLock null
        }

        // The API rotates refresh tokens on every refresh and returns the new one as a cookie.
        // Persist it immediately so process restarts do not keep an already-invalidated token.
        val rotatedRefreshToken = authApiService.getRefreshToken() ?: storedRefreshToken.orEmpty()
        authManager.saveTokens(accessToken, rotatedRefreshToken, expiresIn = 3600)
        accessToken
    }
}
