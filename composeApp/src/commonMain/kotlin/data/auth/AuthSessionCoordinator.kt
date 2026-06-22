package data.auth

import data.repository.CloudSyncedLocalDataCleaner

class AuthSessionCoordinator(
    private val authManager: AuthManager,
    private val cloudSyncedLocalDataCleaner: CloudSyncedLocalDataCleaner,
) {
    suspend fun endSession() {
        cloudSyncedLocalDataCleaner.clearOnLogout()
        authManager.clearTokens()
    }
}
