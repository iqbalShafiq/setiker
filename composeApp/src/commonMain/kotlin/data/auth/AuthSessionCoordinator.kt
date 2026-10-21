package data.auth

import data.repository.CloudSyncedLocalDataCleaner
import domain.error.detailText
import domain.error.onFailure
import util.AppLog

class AuthSessionCoordinator(
    private val authManager: AuthManager,
    private val cloudSyncedLocalDataCleaner: CloudSyncedLocalDataCleaner,
    private val authApiService: AuthApiService,
) {
    suspend fun endSession() {
        val accessToken = runCatching { authManager.getAccessToken() }.getOrNull()
        val refreshToken = runCatching { authManager.getRefreshToken() }.getOrNull()
            ?: authApiService.getRefreshToken()
        authApiService.logout(accessToken = accessToken, refreshToken = refreshToken)
            .onFailure { failure ->
                // R5-adjacent: logout is best-effort; a server-side failure must not block
                // local cleanup, but it should no longer disappear silently.
                AppLog.w("AuthSession", "logout call failed: ${failure.detailText()}")
            }
        cloudSyncedLocalDataCleaner.clearOnLogout()
        authManager.clearTokens()
        authApiService.clearRefreshToken()
    }
}
