package data.auth

import domain.error.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Refreshes the access token at most once per call (R2: no retry loop). Callers
 * (Cloud/Explore planes) treat a null result as "unauthenticated" and surface a clean failure.
 */
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

        val response = when (val refreshOutcome = authApiService.refreshToken(storedRefreshToken)) {
            is Result.Error -> {
                if (clearTokensOnFailure) authManager.clearTokens()
                return@withLock null
            }
            is Result.Success -> refreshOutcome.data
        }

        val accessToken = response.data?.accessToken
        if (accessToken.isNullOrBlank()) {
            if (clearTokensOnFailure) authManager.clearTokens()
            return@withLock null
        }

        // The API rotates refresh tokens on every refresh and returns the new one as a cookie.
        // Persist it immediately so process restarts do not keep an already-invalidated token.
        val rotatedRefreshToken = authApiService.getRefreshToken() ?: storedRefreshToken.orEmpty()
        val expiresIn = (response.data.expiresIn ?: 3600).toLong()
        authManager.saveTokens(accessToken, rotatedRefreshToken, expiresIn = expiresIn)
        accessToken
    }
}
