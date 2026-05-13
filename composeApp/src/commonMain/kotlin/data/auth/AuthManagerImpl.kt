package data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import domain.model.AuthState
import domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthManagerImpl(
    private val dataStore: DataStore<Preferences>
) : AuthManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _authState = MutableStateFlow(AuthState.UNKNOWN)
    override val authState: StateFlow<AuthState> = _authState
    
    override val currentUser: Flow<User?> = dataStore.data.map { preferences ->
        preferences[KEY_USER]?.let { runCatching { json.decodeFromString<User>(it) }.getOrNull() }
    }
    
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
        private val KEY_USER = stringPreferencesKey("user_json")
    }
    
    init {
        CoroutineScope(Dispatchers.Default).launch {
            val isAuth = isAuthenticated()
            _authState.value = if (isAuth) AuthState.AUTHENTICATED else AuthState.UNAUTHENTICATED
        }
    }
    
    override suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
            preferences[KEY_REFRESH_TOKEN] = refreshToken
            preferences[KEY_TOKEN_EXPIRES_AT] = expiresAt
        }
        _authState.value = AuthState.AUTHENTICATED
    }
    
    override suspend fun getAccessToken(): String? {
        return dataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()
    }
    
    override suspend fun getRefreshToken(): String? {
        return dataStore.data.map { it[KEY_REFRESH_TOKEN] }.first()
    }
    
    override suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_REFRESH_TOKEN)
            preferences.remove(KEY_TOKEN_EXPIRES_AT)
            preferences.remove(KEY_USER)
        }
        _authState.value = AuthState.UNAUTHENTICATED
    }
    
    override suspend fun isAuthenticated(): Boolean {
        val token = getAccessToken()
        val expiresAt = dataStore.data.map { it[KEY_TOKEN_EXPIRES_AT] ?: 0L }.first()
        return !token.isNullOrBlank() && expiresAt > System.currentTimeMillis()
    }
    
    override suspend fun saveUser(user: User) {
        dataStore.edit { preferences ->
            preferences[KEY_USER] = json.encodeToString(user)
        }
    }
    
    override suspend fun getUser(): User? {
        return dataStore.data.map { preferences ->
            preferences[KEY_USER]?.let { runCatching { json.decodeFromString<User>(it) }.getOrNull() }
        }.first()
    }
    
    override suspend fun updateAccessToken(newToken: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = newToken
        }
    }
    
    override suspend fun getValidAccessToken(): String? {
        val token = getAccessToken() ?: return null
        val expiresAt = dataStore.data.map { it[KEY_TOKEN_EXPIRES_AT] ?: 0L }.first()
        if (expiresAt <= System.currentTimeMillis() + 300_000) {
            return null
        }
        return token
    }
}