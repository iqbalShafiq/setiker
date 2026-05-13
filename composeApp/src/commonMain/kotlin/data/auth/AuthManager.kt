package data.auth

import domain.model.User
import domain.model.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthManager {
    val authState: StateFlow<AuthState>
    val currentUser: Flow<User?>
    
    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
    suspend fun isAuthenticated(): Boolean
    suspend fun saveUser(user: User)
    suspend fun getUser(): User?
    suspend fun updateAccessToken(newToken: String)
    fun isTokenExpired(): Boolean
    suspend fun getValidAccessToken(): String?
}