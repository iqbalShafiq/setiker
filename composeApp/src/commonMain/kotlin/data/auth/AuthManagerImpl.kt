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
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.long
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
        val expiresAt = parseJwtExpiration(accessToken)
            ?: (Clock.System.now().toEpochMilliseconds() + (expiresIn * 1000))
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
        val refreshToken = getRefreshToken()
        val expiresAt = dataStore.data.map { it[KEY_TOKEN_EXPIRES_AT] ?: 0L }.first()
        return !refreshToken.isNullOrBlank() ||
            (!token.isNullOrBlank() && expiresAt > Clock.System.now().toEpochMilliseconds())
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
        val expiresAtFromToken = parseJwtExpiration(newToken)
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = newToken
            expiresAtFromToken?.let { preferences[KEY_TOKEN_EXPIRES_AT] = it }
        }
    }
    
     override fun isTokenExpired(): Boolean {
         val expiresAt = runBlocking {
             dataStore.data.map { it[KEY_TOKEN_EXPIRES_AT] ?: 0L }.first()
         }
         return expiresAt <= Clock.System.now().toEpochMilliseconds() + 300_000
     }
     
      override suspend fun getValidAccessToken(): String? {
          val token = getAccessToken() ?: return null
          if (isTokenExpired()) {
              return null
          }
          return token
      }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseJwtExpiration(token: String): Long? {
        val payloadPart = token.split('.').getOrNull(1) ?: return null
        val payloadJson = runCatching {
            Base64.UrlSafe.decode(payloadPart).decodeToString()
        }.getOrNull() ?: return null
        val expSeconds = runCatching {
            json.parseToJsonElement(payloadJson)
                .jsonObject["exp"]
                ?.jsonPrimitive
                ?.long
        }.getOrNull() ?: return null
        return expSeconds * 1000
    }
  }
