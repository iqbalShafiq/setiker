package data.auth

import data.repository.CloudSyncedLocalDataCleaner

class AuthSessionCoordinator(
    private val authManager: AuthManager,
    private val cloudSyncedLocalDataCleaner: CloudSyncedLocalDataCleaner,
    private val authApiService: AuthApiService,
) {
    suspend fun endSession() {
        val accessToken = runCatching { authManager.getAccessToken() }.getOrNull()
        val refreshToken = runCatching { authManager.getRefreshToken() }.getOrNull()
            ?: authApiService.getRefreshToken()
        runCatching {
            authApiService.logout(accessToken = accessToken, refreshToken = refreshToken)
        }
        cloudSyncedLocalDataCleaner.clearOnLogout()
        authManager.clearTokens()
        authApiService.clearRefreshToken()
    }
}
