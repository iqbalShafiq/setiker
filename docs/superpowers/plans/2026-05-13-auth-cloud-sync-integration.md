# Auth & Cloud Sync Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate the Setiker KMP app with the new WhatsApp Sticker API authentication system and cloud sync capabilities while maintaining full offline functionality.

**Architecture:** Unified Repository with Auto-Sync (Approach C). All operations write to local DB first, then auto-sync to cloud via background workers. Server wins for conflicts.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor client, Room (SQLDelight for KMP), Koin DI, kotlinx-datetime, DataStore, WorkManager (Android), BackgroundTasks (iOS)

---

## File Structure

### New Files (Create)

**Domain Layer:**
- `domain/model/User.kt` - User domain model
- `domain/model/SyncStatus.kt` - Sync status enum
- `domain/model/SyncOperation.kt` - Sync operation model
- `domain/model/SyncReport.kt` - Sync report model

**Data Layer - Auth:**
- `data/auth/AuthManager.kt` - Token management interface
- `data/auth/AuthManagerImpl.kt` - Token management implementation
- `data/auth/AuthApiService.kt` - Auth API calls
- `data/auth/model/AuthModels.kt` - Auth DTOs
- `data/auth/model/UserMapper.kt` - User mapping

**Data Layer - Cloud:**
- `data/remote/CloudStickerRepository.kt` - Cloud API wrapper
- `data/remote/model/CloudModels.kt` - Cloud API DTOs
- `data/remote/model/CloudMappers.kt` - Cloud model mapping

**Data Layer - Sync:**
- `data/sync/SyncManager.kt` - Sync engine interface
- `data/sync/SyncManagerImpl.kt` - Sync engine implementation
- `data/sync/SyncWorker.kt` - Background sync worker (expect/actual)
- `data/sync/NetworkMonitor.kt` - Network state monitoring

**Data Layer - Local:**
- `data/local/entity/PendingSyncOperationEntity.kt` - Sync queue entity
- `data/local/database/SyncOperationDao.kt` - Sync queue DAO

**Presentation Layer:**
- `presentation/auth/LoginScreen.kt` - Login screen
- `presentation/auth/LoginViewModel.kt` - Login ViewModel
- `presentation/auth/LoginState.kt` - Login state
- `presentation/auth/LoginIntent.kt` - Login intents
- `presentation/auth/RegisterScreen.kt` - Register screen
- `presentation/auth/RegisterViewModel.kt` - Register ViewModel
- `presentation/auth/ProfileScreen.kt` - Profile bottom sheet
- `presentation/auth/ProfileViewModel.kt` - Profile ViewModel
- `presentation/sync/SyncScreen.kt` - Sync status screen
- `presentation/sync/SyncViewModel.kt` - Sync ViewModel
- `presentation/sync/SyncState.kt` - Sync state
- `presentation/sync/SyncIntent.kt` - Sync intents
- `presentation/components/SyncStatusIndicator.kt` - Sync indicator badge
- `presentation/components/HomeBottomBar.kt` - Home bottom app bar

### Modified Files (Edit)

**Data Layer:**
- `data/local/entity/StickerPackEntity.kt` - Add sync columns
- `data/local/entity/StickerEntity.kt` - Add sync columns
- `data/local/database/StickerDatabase.kt` - Bump version, add entities
- `data/local/database/StickerPackDao.kt` - Add sync queries
- `data/local/database/StickerDao.kt` - Add sync queries
- `data/remote/SetikerApiService.kt` - Add auth interceptor
- `data/remote/StickerApiRepository.kt` - Add cloud operations
- `data/remote/model/ApiEnvelope.kt` - Add auth response models
- `data/repository/StickerRepositoryImpl.kt` - Add cloud sync logic
- `domain/repository/StickerRepository.kt` - Add sync methods

**Presentation Layer:**
- `presentation/home/HomeScreen.kt` - Add bottom bar, sync indicators
- `presentation/home/HomeViewModel.kt` - Add sync logic
- `presentation/home/HomeState.kt` - Add auth/sync state
- `presentation/home/HomeIntent.kt` - Add sync intents
- `presentation/packdetail/PackDetailScreen.kt` - Add sync status icon
- `presentation/navigation/AppNavigation.kt` - Add auth/sync routes
- `presentation/navigation/Screen.kt` - Add new screens

**DI Layer:**
- `di/AppModule.kt` - Register new dependencies
- `di/AppModule.android.kt` - Android-specific (WorkManager)
- `di/AppModule.ios.kt` - iOS-specific (BackgroundTasks)

**Build:**
- `build.gradle.kts` (composeApp) - Add dependencies
- `gradle/libs.versions.toml` - Add library versions

---

## Phase 1: Foundation

### Task 1.1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add library versions to TOML**

Open `gradle/libs.versions.toml` and add:

```toml
[versions]
# Add these new versions
kotlinx-datetime = "0.6.1"
datastore = "1.1.1"
androidx-security = "1.1.0-alpha06"
androidx-work = "2.9.1"

[libraries]
# Add these new libraries
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "androidx-security" }
androidx-work-runtime = { module = "androidx.work:work-runtime-ktx", version.ref = "androidx-work" }
```

- [ ] **Step 2: Add dependencies to build.gradle.kts**

Open `composeApp/build.gradle.kts` and in the `commonMain.dependencies` block add:

```kotlin
implementation(libs.kotlinx.datetime)
implementation(libs.androidx.datastore.preferences)
```

In the `androidMain.dependencies` block add:

```kotlin
implementation(libs.androidx.security.crypto)
implementation(libs.androidx.work.runtime)
```

- [ ] **Step 3: Sync project and verify build**

Run: `./gradlew composeApp:dependencies --configuration commonMainCompileDependenciesMetadata | grep -E "datetime|datastore"`

Expected: See kotlinx-datetime and datastore in the output.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build: add kotlinx-datetime, datastore, security-crypto, work-manager dependencies"
```

---

### Task 1.2: Create Domain Models for Auth and Sync

**Files:**
- Create: `composeApp/src/commonMain/kotlin/domain/model/User.kt`
- Create: `composeApp/src/commonMain/kotlin/domain/model/SyncStatus.kt`
- Create: `composeApp/src/commonMain/kotlin/domain/model/SyncOperation.kt`
- Create: `composeApp/src/commonMain/kotlin/domain/model/SyncReport.kt`

- [ ] **Step 1: Create User model**

Create `domain/model/User.kt`:

```kotlin
package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val name: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Long // epoch millis UTC
)

@Serializable
data class UserRole(
    val id: String,
    val name: String
)

enum class AuthState {
    UNKNOWN,
    UNAUTHENTICATED,
    AUTHENTICATED
}
```

- [ ] **Step 2: Create SyncStatus enum**

Create `domain/model/SyncStatus.kt`:

```kotlin
package domain.model

enum class SyncState {
    LOCAL_ONLY,
    SYNCED,
    PENDING,
    FAILED
}

enum class SyncOperationType {
    CREATE_PACK,
    UPDATE_PACK,
    DELETE_PACK,
    ADD_STICKER,
    UPDATE_STICKER,
    DELETE_STICKER,
    UPLOAD_IMAGE,
    REORDER_STICKERS
}

enum class SyncOperationStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    CANCELLED
}
```

- [ ] **Step 3: Create SyncOperation model**

Create `domain/model/SyncOperation.kt`:

```kotlin
package domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SyncOperation(
    val id: String,
    val type: SyncOperationType,
    val targetId: String,
    val payload: String, // JSON serialized
    val status: SyncOperationStatus,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long, // epoch millis UTC
    val completedAt: Long? = null,
    val priority: Int = 0
)
```

- [ ] **Step 4: Create SyncReport model**

Create `domain/model/SyncReport.kt`:

```kotlin
package domain.model

sealed class SyncResult {
    data object Success : SyncResult()
    data class Failed(val error: String) : SyncResult()
    data object SkippedOffline : SyncResult()
    data object SkippedNotAuthenticated : SyncResult()
}

data class SyncReport(
    val operationsProcessed: Int = 0,
    val operationsSucceeded: Int = 0,
    val operationsFailed: Int = 0,
    val packsSynced: Int = 0,
    val stickersSynced: Int = 0,
    val result: SyncResult = SyncResult.Success,
    val timestamp: Long = System.currentTimeMillis()
)
```

- [ ] **Step 5: Commit**

```bash
git add domain/model/User.kt domain/model/SyncStatus.kt domain/model/SyncOperation.kt domain/model/SyncReport.kt
git commit -m "feat: add domain models for auth and sync"
```

---

### Task 1.3: Database Migration - Add Sync Columns

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/data/local/entity/StickerPackEntity.kt`
- Modify: `composeApp/src/commonMain/kotlin/data/local/entity/StickerEntity.kt`
- Modify: `composeApp/src/commonMain/kotlin/data/local/database/StickerDatabase.kt`

- [ ] **Step 1: Add sync columns to StickerPackEntity**

Open `data/local/entity/StickerPackEntity.kt` and add fields:

```kotlin
@Entity(tableName = "sticker_pack")
data class StickerPackEntity(
    @PrimaryKey val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val isAnimated: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    // New sync columns
    val cloudId: String? = null,
    val syncState: String = "LOCAL_ONLY", // LOCAL_ONLY, SYNCED, PENDING, FAILED
    val lastSyncAt: Long? = null,
    val visibility: String = "PRIVATE", // PUBLIC, PRIVATE, UNLISTED
    val cloudOwnerId: String? = null
)
```

- [ ] **Step 2: Add sync columns to StickerEntity**

Open `data/local/entity/StickerEntity.kt` and add fields:

```kotlin
@Entity(
    tableName = "sticker",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StickerEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val imageFile: String,
    val sourceImageFile: String? = null,
    val emojis: String = "[]",
    val accessibilityText: String? = null,
    val decorationsJson: String? = null,
    val isAnimated: Boolean = false,
    val sourceVideoFile: String? = null,
    val frameDecorationsJson: String? = null,
    val sortOrder: Int = 0,
    // New sync columns
    val cloudId: String? = null,
    val syncState: String = "LOCAL_ONLY",
    val lastSyncAt: Long? = null,
    val cloudUrl: String? = null
)
```

- [ ] **Step 3: Bump database version and add entities**

Open `data/local/database/StickerDatabase.kt`:

```kotlin
@Database(
    entities = [
        StickerPackEntity::class, 
        StickerEntity::class,
        PendingSyncOperationEntity::class // We'll create this next
    ],
    version = 5, // Bump from 4 to 5
    exportSchema = false
)
abstract class StickerDatabase : RoomDatabase() {
    abstract fun stickerPackDao(): StickerPackDao
    abstract fun stickerDao(): StickerDao
    abstract fun syncOperationDao(): SyncOperationDao // New DAO
}
```

- [ ] **Step 4: Commit**

```bash
git add data/local/entity/StickerPackEntity.kt data/local/entity/StickerEntity.kt data/local/database/StickerDatabase.kt
git commit -m "feat: add sync columns to sticker entities and bump DB version to 5"
```

---

### Task 1.4: Create Sync Queue Entity and DAO

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/local/entity/PendingSyncOperationEntity.kt`
- Create: `composeApp/src/commonMain/kotlin/data/local/database/SyncOperationDao.kt`

- [ ] **Step 1: Create PendingSyncOperationEntity**

Create `data/local/entity/PendingSyncOperationEntity.kt`:

```kotlin
package data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["targetId"]),
        Index(value = ["createdAt"])
    ]
)
data class PendingSyncOperationEntity(
    @PrimaryKey val id: String,
    val type: String, // SyncOperationType name
    val targetId: String,
    val payload: String,
    val status: String = "PENDING", // SyncOperationStatus name
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long,
    val completedAt: Long? = null,
    val priority: Int = 0
)
```

- [ ] **Step 2: Create SyncOperationDao**

Create `data/local/database/SyncOperationDao.kt`:

```kotlin
package data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import data.local.entity.PendingSyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingSyncOperationEntity)

    @Update
    suspend fun update(operation: PendingSyncOperationEntity)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY priority DESC, createdAt ASC")
    suspend fun getPending(): List<PendingSyncOperationEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'IN_PROGRESS', 'FAILED') ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingSyncOperationEntity>>

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: String): PendingSyncOperationEntity?

    @Query("UPDATE sync_queue SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: String, status: String, completedAt: Long)

    @Query("UPDATE sync_queue SET status = 'FAILED', errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)

    @Query("DELETE FROM sync_queue WHERE status = 'SUCCESS' AND completedAt < :beforeTimestamp")
    suspend fun deleteCompletedBefore(beforeTimestamp: Long)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'IN_PROGRESS')")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
```

- [ ] **Step 3: Commit**

```bash
git add data/local/entity/PendingSyncOperationEntity.kt data/local/database/SyncOperationDao.kt
git commit -m "feat: add sync queue entity and DAO"
```

---

### Task 1.5: Create AuthManager Interface and DataStore Token Storage

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/auth/AuthManager.kt`
- Create: `composeApp/src/commonMain/kotlin/data/auth/AuthManagerImpl.kt`

- [ ] **Step 1: Create AuthManager interface**

Create `data/auth/AuthManager.kt`:

```kotlin
package data.auth

import domain.model.User
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

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long // epoch millis when token expires
)

sealed class AuthManagerState {
    data object Unknown : AuthManagerState()
    data object Unauthenticated : AuthManagerState()
    data class Authenticated(val user: User) : AuthManagerState()
}
```

- [ ] **Step 2: Create AuthManagerImpl with DataStore**

Create `data/auth/AuthManagerImpl.kt`:

```kotlin
package data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthManagerImpl(
    private val dataStore: DataStore<Preferences>
) : AuthManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.UNKNOWN)
    override val authState: StateFlow<AuthState> = _authState
    
    override val currentUser: Flow<User?> = dataStore.data.map { preferences ->
        preferences[KEY_USER]?.let { json.decodeFromString(it) }
    }
    
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
        private val KEY_USER = stringPreferencesKey("user_json")
    }
    
    init {
        // Initialize auth state on creation
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
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
            preferences[KEY_USER]?.let { json.decodeFromString(it) }
        }.first()
    }
    
    override suspend fun updateAccessToken(newToken: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = newToken
        }
    }
    
    override fun isTokenExpired(): Boolean {
        val expiresAt = runBlocking { 
            dataStore.data.map { it[KEY_TOKEN_EXPIRES_AT] ?: 0L }.first() 
        }
        return expiresAt <= System.currentTimeMillis() + 300_000 // 5 min buffer
    }
    
    override suspend fun getValidAccessToken(): String? {
        val token = getAccessToken() ?: return null
        if (isTokenExpired()) {
            // Token expired - caller should handle refresh
            return null
        }
        return token
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add data/auth/AuthManager.kt data/auth/AuthManagerImpl.kt
git commit -m "feat: add AuthManager with DataStore token storage"
```

---

### Task 1.6: Create NetworkMonitor

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/sync/NetworkMonitor.kt`
- Create: `composeApp/src/androidMain/kotlin/data/sync/NetworkMonitor.android.kt`
- Create: `composeApp/src/iosMain/kotlin/data/sync/NetworkMonitor.ios.kt`

- [ ] **Step 1: Create expect declaration**

Create `data/sync/NetworkMonitor.kt`:

```kotlin
package data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

expect class NetworkMonitor {
    val isOnline: StateFlow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
}
```

- [ ] **Step 2: Create Android actual implementation**

Create `data/sync/NetworkMonitor.android.kt`:

```kotlin
package data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

actual class NetworkMonitor(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isOnline = MutableStateFlow(checkCurrentNetwork())
    actual val isOnline: StateFlow<Boolean> = _isOnline
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }
        
        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentNetwork()
        }
        
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
    
    actual fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
    
    actual fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    
    private fun checkCurrentNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

- [ ] **Step 3: Create iOS actual implementation**

Create `data/sync/NetworkMonitor.ios.kt`:

```kotlin
package data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Network.*
import platform.darwin.dispatch_queue_create

actual class NetworkMonitor {
    private val _isOnline = MutableStateFlow(false)
    actual val isOnline: StateFlow<Boolean> = _isOnline
    
    private var monitor: nw_path_monitor_t? = null
    private val monitorQueue = dispatch_queue_create("NetworkMonitor", null)
    
    actual fun startMonitoring() {
        monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor!!, monitorQueue)
        
        nw_path_monitor_set_update_handler(monitor!!) { path ->
            val isOnline = nw_path_get_status(path) == nw_path_status_satisfied
            _isOnline.value = isOnline
        }
        
        nw_path_monitor_start(monitor!!)
    }
    
    actual fun stopMonitoring() {
        monitor?.let {
            nw_path_monitor_cancel(it)
            monitor = null
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add data/sync/NetworkMonitor.kt data/sync/NetworkMonitor.android.kt data/sync/NetworkMonitor.ios.kt
git commit -m "feat: add NetworkMonitor with Android and iOS implementations"
```

---

## Phase 2: Cloud Repository & Auth API

### Task 2.1: Create Auth API Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/auth/model/AuthModels.kt`

- [ ] **Step 1: Create auth DTOs**

Create `data/auth/model/AuthModels.kt`:

```kotlin
package data.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val name: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val data: AuthData? = null
)

@Serializable
data class AuthData(
    val user: UserDto,
    val tokens: TokenDto
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val name: String? = null,
    val roleId: String,
    val role: RoleDto? = null,
    val isActive: Boolean,
    val createdAt: String // ISO 8601
)

@Serializable
data class RoleDto(
    val id: String,
    val name: String
)

@Serializable
data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long // seconds
)

@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val data: UserDto? = null
)
```

- [ ] **Step 2: Create UserMapper**

Create `data/auth/model/UserMapper.kt`:

```kotlin
package data.auth.model

import domain.model.User
import domain.model.UserRole
import kotlinx.datetime.Instant

fun UserDto.toDomainModel(): User {
    return User(
        id = id,
        email = email,
        username = username,
        name = name,
        role = role?.toDomainModel() ?: UserRole(id = roleId, name = "user"),
        isActive = isActive,
        createdAt = Instant.parse(createdAt).toEpochMilliseconds()
    )
}

fun RoleDto.toDomainModel(): UserRole {
    return UserRole(
        id = id,
        name = name
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add data/auth/model/AuthModels.kt data/auth/model/UserMapper.kt
git commit -m "feat: add auth API DTOs and user mapper"
```

---

### Task 2.2: Create AuthApiService

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/auth/AuthApiService.kt`

- [ ] **Step 1: Create AuthApiService**

Create `data/auth/AuthApiService.kt`:

```kotlin
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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
            val error = runCatching { json.decodeFromString<AuthResponse>(bodyText) }.getOrNull()
            throw ApiException(
                code = AppErrorCode.AuthRegisterFailed,
                message = error?.data?.toString() ?: "Registration failed"
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
            val error = runCatching { json.decodeFromString<AuthResponse>(bodyText) }.getOrNull()
            throw ApiException(
                code = AppErrorCode.AuthLoginFailed,
                message = error?.data?.toString() ?: "Login failed"
            )
        }
        
        return json.decodeFromString(bodyText)
    }
    
    suspend fun refreshToken(): AuthResponse {
        val response = client.post("$baseUrl/api/v1/auth/refresh")
        
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(
                code = AppErrorCode.AuthRefreshFailed,
                message = "Token refresh failed"
            )
        }
        
        return json.decodeFromString(bodyText)
    }
    
    suspend fun logout() {
        client.post("$baseUrl/api/v1/auth/logout")
    }
    
    suspend fun getProfile(token: String): UserProfileResponse {
        val response = client.get("$baseUrl/api/v1/auth/me") {
            headers { append("Authorization", "Bearer $token") }
        }
        
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(
                code = AppErrorCode.AuthProfileFailed,
                message = "Failed to get profile"
            )
        }
        
        return json.decodeFromString(bodyText)
    }
    
    suspend fun changePassword(token: String, request: ChangePasswordRequest) {
        val response = client.post("$baseUrl/api/v1/auth/change-password") {
            contentType(ContentType.Application.Json)
            headers { append("Authorization", "Bearer $token") }
            setBody(request)
        }
        
        if (!response.status.isSuccess()) {
            val bodyText = response.bodyAsText()
            val error = runCatching { json.decodeFromString<AuthResponse>(bodyText) }.getOrNull()
            throw ApiException(
                code = AppErrorCode.AuthChangePasswordFailed,
                message = error?.data?.toString() ?: "Password change failed"
            )
        }
    }
}
```

- [ ] **Step 2: Add new error codes**

Open `domain/error/AppErrorCode.kt` and add:

```kotlin
// Auth errors
AuthLoginFailed("AUTH_LOGIN_FAILED"),
AuthRegisterFailed("AUTH_REGISTER_FAILED"),
AuthRefreshFailed("AUTH_REFRESH_FAILED"),
AuthProfileFailed("AUTH_PROFILE_FAILED"),
AuthChangePasswordFailed("AUTH_CHANGE_PASSWORD_FAILED"),
AuthNotAuthenticated("AUTH_NOT_AUTHENTICATED"),
```

- [ ] **Step 3: Commit**

```bash
git add data/auth/AuthApiService.kt domain/error/AppErrorCode.kt
git commit -m "feat: add AuthApiService with login, register, refresh, and profile endpoints"
```

---

### Task 2.3: Enhance SetikerApiService with Auth Interceptor

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/data/remote/SetikerApiService.kt`

- [ ] **Step 1: Add auth interceptor to Ktor client**

Open `data/remote/SetikerApiService.kt` and refactor:

```kotlin
package data.remote

import data.auth.AuthManager
import data.remote.model.ApiErrorEnvelope
import data.remote.model.ApiImage
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.BackgroundRemoveData
import data.remote.model.GenerateData
import data.remote.model.GridSplitData
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class SetikerApiService(
    private val authManager: AuthManager? = null,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        if (ApiConfig.isDebugLoggingEnabled) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("KtorClient: $message")
                    }
                }
                level = LogLevel.ALL
            }
        }
        install(DefaultRequest) {
            url(baseUrl)
            if (url.protocol.name.isBlank()) {
                url.protocol = URLProtocol.HTTP
            }
        }
    }
    
    // ... existing methods (removeBackground, generate, splitGrid) remain the same
    // but need to add auth header
    
    suspend fun removeBackground(imagePath: String): ApiImage {
        val response = client.post("/api/v1/background/remove") {
            addAuthHeader()
            setMultipartBody(imagePath = imagePath)
        }
        // ... rest remains same
    }
    
    suspend fun generate(/* ... */): List<ApiImage> {
        val response = client.post("/api/v1/generate") {
            addAuthHeader()
            // ... rest remains same
        }
        // ... rest remains same
    }
    
    private fun io.ktor.client.request.HttpRequestBuilder.addAuthHeader() {
        authManager?.let { manager ->
            runBlocking {
                manager.getAccessToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}
```

Note: You'll need to add `import kotlinx.coroutines.runBlocking` at the top.

- [ ] **Step 2: Commit**

```bash
git add data/remote/SetikerApiService.kt
git commit -m "feat: add auth header interceptor to SetikerApiService"
```

---

### Task 2.4: Create Cloud Sticker Repository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/remote/CloudStickerRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/model/CloudModels.kt`

- [ ] **Step 1: Create cloud API DTOs**

Create `data/remote/model/CloudModels.kt`:

```kotlin
package data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class StickerPackDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val visibility: String, // PUBLIC, PRIVATE, UNLISTED
    val stickers: List<StickerPackStickerDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null
)

@Serializable
data class StickerPackStickerDto(
    val id: String,
    val stickerPackId: String,
    val stickerId: String,
    val sticker: StickerDto? = null,
    val order: Int = 0,
    val createdAt: String
)

@Serializable
data class StickerDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val url: String,
    val tags: List<String> = emptyList(),
    val visibility: String,
    val userId: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null
)

@Serializable
data class CreateStickerPackRequest(
    val name: String,
    val description: String? = null,
    val visibility: String = "PRIVATE",
    val stickers: List<StickerPackStickerInput> = emptyList()
)

@Serializable
data class StickerPackStickerInput(
    val name: String,
    val filename: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val order: Int = 0
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val data: SyncData? = null
)

@Serializable
data class SyncData(
    val stickerPacks: SyncDelta? = null,
    val stickers: SyncDelta? = null,
    val syncToken: String? = null
)

@Serializable
data class SyncDelta(
    val created: List<StickerPackDto> = emptyList(),
    val updated: List<StickerPackDto> = emptyList(),
    val deleted: List<StickerPackDto> = emptyList()
)
```

- [ ] **Step 2: Create CloudStickerRepository**

Create `data/remote/CloudStickerRepository.kt`:

```kotlin
package data.remote

import data.auth.AuthManager
import data.remote.model.CloudModels
import data.remote.model.StickerPackDto
import data.remote.model.SyncResponse
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class CloudStickerRepository(
    private val authManager: AuthManager,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }
    
    private suspend fun getAuthHeader(): String {
        val token = authManager.getAccessToken()
            ?: throw ApiException(code = AppErrorCode.AuthNotAuthenticated)
        return "Bearer $token"
    }
    
    suspend fun getMyPacks(): List<StickerPackDto> {
        val response = client.get("$baseUrl/api/v1/sticker-packs") {
            header(HttpHeaders.Authorization, getAuthHeader())
        }
        
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudFetchFailed)
        }
        
        val envelope = json.decodeFromString<ApiSuccessEnvelope<List<StickerPackDto>>>(response.bodyAsText())
        return envelope.data ?: emptyList()
    }
    
    suspend fun createPack(request: CreateStickerPackRequest): StickerPackDto {
        val response = client.post("$baseUrl/api/v1/sticker-packs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, getAuthHeader())
            setBody(request)
        }
        
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudCreateFailed)
        }
        
        val envelope = json.decodeFromString<ApiSuccessEnvelope<StickerPackDto>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudCreateFailed)
    }
    
    suspend fun updatePack(packId: String, request: CreateStickerPackRequest): StickerPackDto {
        val response = client.put("$baseUrl/api/v1/sticker-packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, getAuthHeader())
            setBody(request)
        }
        
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudUpdateFailed)
        }
        
        val envelope = json.decodeFromString<ApiSuccessEnvelope<StickerPackDto>>(response.bodyAsText())
        return envelope.data ?: throw ApiException(code = AppErrorCode.CloudUpdateFailed)
    }
    
    suspend fun deletePack(packId: String) {
        val response = client.delete("$baseUrl/api/v1/sticker-packs/$packId") {
            header(HttpHeaders.Authorization, getAuthHeader())
        }
        
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudDeleteFailed)
        }
    }
    
    suspend fun sync(lastSyncAt: Long?): SyncResponse {
        val url = if (lastSyncAt != null) {
            "$baseUrl/api/v1/sync?lastSyncAt=${lastSyncAt}"
        } else {
            "$baseUrl/api/v1/sync"
        }
        
        val response = client.get(url) {
            header(HttpHeaders.Authorization, getAuthHeader())
        }
        
        if (!response.status.isSuccess()) {
            throw ApiException(code = AppErrorCode.CloudSyncFailed)
        }
        
        return json.decodeFromString(response.bodyAsText())
    }
    
    suspend fun uploadImages(
        packId: String,
        imagePaths: List<String>
    ): List<StickerDto> {
        // Implementation for multipart upload
        // This is a placeholder - actual implementation depends on FileBridge
        throw NotImplementedError("Upload images requires FileBridge integration")
    }
}
```

- [ ] **Step 3: Add cloud error codes**

Open `domain/error/AppErrorCode.kt` and add:

```kotlin
// Cloud errors
CloudFetchFailed("CLOUD_FETCH_FAILED"),
CloudCreateFailed("CLOUD_CREATE_FAILED"),
CloudUpdateFailed("CLOUD_UPDATE_FAILED"),
CloudDeleteFailed("CLOUD_DELETE_FAILED"),
CloudSyncFailed("CLOUD_SYNC_FAILED"),
```

- [ ] **Step 4: Commit**

```bash
git add data/remote/CloudStickerRepository.kt data/remote/model/CloudModels.kt domain/error/AppErrorCode.kt
git commit -m "feat: add CloudStickerRepository with pack CRUD and sync endpoints"
```

---

## Phase 3: Sync Engine

### Task 3.1: Create SyncManager

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/sync/SyncManager.kt`
- Create: `composeApp/src/commonMain/kotlin/data/sync/SyncManagerImpl.kt`

- [ ] **Step 1: Create SyncManager interface**

Create `data/sync/SyncManager.kt`:

```kotlin
package data.sync

import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncManager {
    val operationsFlow: Flow<List<SyncOperation>>
    val activeOperationFlow: StateFlow<SyncOperation?>
    val isSyncing: StateFlow<Boolean>
    val lastSyncReport: StateFlow<SyncReport?>
    
    suspend fun enqueue(operation: SyncOperation)
    suspend fun processQueue(): SyncReport
    suspend fun retry(operationId: String)
    suspend fun cancel(operationId: String)
    suspend fun clearCompleted()
    suspend fun sync(): SyncReport
    fun startMonitoring()
    fun stopMonitoring()
}
```

- [ ] **Step 2: Create SyncManagerImpl**

Create `data/sync/SyncManagerImpl.kt`:

```kotlin
package data.sync

import data.auth.AuthManager
import data.local.database.SyncOperationDao
import data.local.entity.PendingSyncOperationEntity
import data.remote.CloudStickerRepository
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncOperationType
import domain.model.SyncReport
import domain.model.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SyncManagerImpl(
    private val operationDao: SyncOperationDao,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val networkMonitor: NetworkMonitor,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SyncManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override val operationsFlow: Flow<List<SyncOperation>> = 
        operationDao.observeAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    
    private val _activeOperationFlow = MutableStateFlow<SyncOperation?>(null)
    override val activeOperationFlow: StateFlow<SyncOperation?> = _activeOperationFlow
    
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val _lastSyncReport = MutableStateFlow<SyncReport?>(null)
    override val lastSyncReport: StateFlow<SyncReport?> = _lastSyncReport
    
    init {
        startMonitoring()
    }
    
    override fun startMonitoring() {
        coroutineScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline && authManager.isAuthenticated()) {
                    processQueue()
                }
            }
        }
    }
    
    override fun stopMonitoring() {
        // Cancel all coroutines
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun enqueue(operation: SyncOperation) {
        val entity = operation.toEntity()
        operationDao.insert(entity)
        
        // Try to process immediately if online
        if (networkMonitor.isOnline.value && authManager.isAuthenticated()) {
            processQueue()
        }
    }
    
    override suspend fun processQueue(): SyncReport {
        if (_isSyncing.value) {
            return SyncReport(result = SyncResult.SkippedOffline)
        }
        
        if (!authManager.isAuthenticated()) {
            return SyncReport(result = SyncResult.SkippedNotAuthenticated)
        }
        
        if (!networkMonitor.isOnline.value) {
            return SyncReport(result = SyncResult.SkippedOffline)
        }
        
        _isSyncing.value = true
        val report = SyncReport()
        
        try {
            val pendingOps = operationDao.getPending()
            var processedCount = 0
            var successCount = 0
            var failedCount = 0
            
            for (entity in pendingOps) {
                val operation = entity.toDomainModel()
                _activeOperationFlow.value = operation
                
                val result = executeOperation(operation)
                processedCount++
                
                when (result) {
                    is OperationResult.Success -> {
                        operationDao.markCompleted(
                            operation.id, 
                            SyncOperationStatus.SUCCESS.name, 
                            System.currentTimeMillis()
                        )
                        successCount++
                    }
                    is OperationResult.RetryableError -> {
                        if (operation.retryCount < 5) {
                            // Will retry later
                            val updatedEntity = entity.copy(
                                retryCount = operation.retryCount + 1
                            )
                            operationDao.update(updatedEntity)
                        } else {
                            operationDao.markFailed(operation.id, result.error)
                            failedCount++
                        }
                    }
                    is OperationResult.PermanentError -> {
                        operationDao.markFailed(operation.id, result.error)
                        failedCount++
                    }
                }
                
                delay(100) // Small delay between operations
            }
            
            val finalReport = report.copy(
                operationsProcessed = processedCount,
                operationsSucceeded = successCount,
                operationsFailed = failedCount,
                result = if (failedCount > 0) SyncResult.Failed("$failedCount operations failed") else SyncResult.Success
            )
            
            _lastSyncReport.value = finalReport
            return finalReport
            
        } finally {
            _isSyncing.value = false
            _activeOperationFlow.value = null
        }
    }
    
    private suspend fun executeOperation(operation: SyncOperation): OperationResult {
        return try {
            when (operation.type) {
                SyncOperationType.CREATE_PACK -> {
                    // Parse payload and create pack
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    cloudRepo.createPack(request)
                    OperationResult.Success
                }
                SyncOperationType.UPDATE_PACK -> {
                    val request = json.decodeFromString<CreateStickerPackRequest>(operation.payload)
                    // Extract packId from targetId or payload
                    cloudRepo.updatePack(operation.targetId, request)
                    OperationResult.Success
                }
                SyncOperationType.DELETE_PACK -> {
                    cloudRepo.deletePack(operation.targetId)
                    OperationResult.Success
                }
                else -> {
                    OperationResult.PermanentError("Operation type ${operation.type} not implemented")
                }
            }
        } catch (e: Exception) {
            when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    OperationResult.RetryableError(e.message ?: "Timeout")
                e.message?.contains("5", ignoreCase = true) == true ->
                    OperationResult.RetryableError(e.message ?: "Server error")
                else ->
                    OperationResult.PermanentError(e.message ?: "Unknown error")
            }
        }
    }
    
    override suspend fun retry(operationId: String) {
        val entity = operationDao.getById(operationId) ?: return
        val updated = entity.copy(status = SyncOperationStatus.PENDING.name)
        operationDao.update(updated)
        processQueue()
    }
    
    override suspend fun cancel(operationId: String) {
        val entity = operationDao.getById(operationId) ?: return
        val updated = entity.copy(status = SyncOperationStatus.CANCELLED.name)
        operationDao.update(updated)
    }
    
    override suspend fun clearCompleted() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        operationDao.deleteCompletedBefore(oneWeekAgo)
    }
    
    override suspend fun sync(): SyncReport {
        return processQueue()
    }
    
    private fun PendingSyncOperationEntity.toDomainModel(): SyncOperation {
        return SyncOperation(
            id = id,
            type = SyncOperationType.valueOf(type),
            targetId = targetId,
            payload = payload,
            status = SyncOperationStatus.valueOf(status),
            errorMessage = errorMessage,
            retryCount = retryCount,
            createdAt = createdAt,
            completedAt = completedAt,
            priority = priority
        )
    }
    
    private fun SyncOperation.toEntity(): PendingSyncOperationEntity {
        return PendingSyncOperationEntity(
            id = id,
            type = type.name,
            targetId = targetId,
            payload = payload,
            status = status.name,
            errorMessage = errorMessage,
            retryCount = retryCount,
            createdAt = createdAt,
            completedAt = completedAt,
            priority = priority
        )
    }
}

sealed class OperationResult {
    data object Success : OperationResult()
    data class RetryableError(val error: String) : OperationResult()
    data class PermanentError(val error: String) : OperationResult()
}
```

Note: You need to import `CreateStickerPackRequest` from the CloudModels or reference it correctly.

- [ ] **Step 3: Commit**

```bash
git add data/sync/SyncManager.kt data/sync/SyncManagerImpl.kt
git commit -m "feat: add SyncManager with queue processing, retry logic, and network monitoring"
```

---

### Task 3.2: Create Background Sync Worker

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/sync/SyncWorker.kt` (expect)
- Create: `composeApp/src/androidMain/kotlin/data/sync/SyncWorker.android.kt`
- Create: `composeApp/src/iosMain/kotlin/data/sync/SyncWorker.ios.kt`

- [ ] **Step 1: Create expect declaration**

Create `data/sync/SyncWorker.kt`:

```kotlin
package data.sync

expect class SyncWorker {
    fun scheduleImmediateSync()
    fun schedulePeriodicSync()
    fun cancelAllSyncWork()
}
```

- [ ] **Step 2: Create Android WorkManager implementation**

Create `data/sync/SyncWorker.android.kt`:

```kotlin
package data.sync

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.TimeUnit

actual class SyncWorker(context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val syncManager: SyncManager by inject(SyncManager::class.java)
    
    actual fun scheduleImmediateSync() {
        val workRequest = OneTimeWorkRequestBuilder<SyncWorkerTask>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    actual fun schedulePeriodicSync() {
        val workRequest = PeriodicWorkRequestBuilder<SyncWorkerTask>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    actual fun cancelAllSyncWork() {
        workManager.cancelUniqueWork("immediate_sync")
        workManager.cancelUniqueWork("periodic_sync")
    }
}

class SyncWorkerTask(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val syncManager: SyncManager by inject(SyncManager::class.java)
    
    override suspend fun doWork(): Result {
        return try {
            val report = syncManager.sync()
            if (report.result is SyncResult.Success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

Note: Need to add imports for `SyncResult` and fix the injection.

- [ ] **Step 3: Create iOS BackgroundTasks implementation**

Create `data/sync/SyncWorker.ios.kt`:

```kotlin
package data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTask
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSCalendar
import platform.UIKit.UIApplication
import kotlin.time.Duration.Companion.minutes

actual class SyncWorker {
    private val taskIdentifier = "com.setiker.sync"
    private val processingTaskIdentifier = "com.setiker.sync.processing"
    
    actual fun scheduleImmediateSync() {
        // iOS doesn't support immediate background execution like Android
        // We'll trigger sync when app comes to foreground instead
        CoroutineScope(Dispatchers.Main).launch {
            // Trigger sync via NotificationCenter or direct call
        }
    }
    
    actual fun schedulePeriodicSync() {
        val request = BGAppRefreshTaskRequest(taskIdentifier)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(15 * 60.0) // 15 minutes
        
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        } catch (e: Exception) {
            println("Failed to schedule background sync: ${e.message}")
        }
    }
    
    actual fun cancelAllSyncWork() {
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add data/sync/SyncWorker.kt data/sync/SyncWorker.android.kt data/sync/SyncWorker.ios.kt
git commit -m "feat: add background sync workers for Android (WorkManager) and iOS (BackgroundTasks)"
```

---

## Phase 4: Enhanced Repository

### Task 4.1: Update StickerRepository Interface

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/domain/repository/StickerRepository.kt`

- [ ] **Step 1: Add sync methods to interface**

Open `domain/repository/StickerRepository.kt`:

```kotlin
package domain.repository

import domain.model.Sticker
import domain.model.StickerPack
import domain.model.SyncReport
import kotlinx.coroutines.flow.Flow

interface StickerRepository {
    // Existing methods
    suspend fun getAllPacks(): List<StickerPack>
    suspend fun getPack(identifier: String): StickerPack
    suspend fun savePack(pack: StickerPack)
    suspend fun deletePack(identifier: String)
    suspend fun addStickerToPack(packId: String, sticker: Sticker)
    suspend fun updateStickerInPack(packId: String, index: Int, sticker: Sticker)
    suspend fun removeStickerFromPack(packId: String, index: Int)
    
    // New sync methods
    suspend fun syncAll(): SyncReport
    suspend fun syncPack(packId: String): SyncReport
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun getPendingSyncCount(): Int
}
```

Note: Need to create `SyncStatus` data class or use the enum we created earlier.

- [ ] **Step 2: Commit**

```bash
git add domain/repository/StickerRepository.kt
git commit -m "feat: add sync methods to StickerRepository interface"
```

---

### Task 4.2: Update StickerRepositoryImpl with Cloud Sync

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/data/repository/StickerRepositoryImpl.kt`

- [ ] **Step 1: Add sync dependencies and methods**

Open `data/repository/StickerRepositoryImpl.kt` and add:

```kotlin
class StickerRepositoryImpl(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val fileStorage: StickerFileStorage,
    private val syncManager: SyncManager? = null,
    private val authManager: AuthManager? = null
) : StickerRepository {
    
    // ... existing methods remain ...
    
    override suspend fun savePack(pack: StickerPack) {
        withContext(Dispatchers.IO) {
            val identifier = pack.identifier.takeIf { it.isNotBlank() } ?: Uuid.random().toString()
            val existing = packDao.getById(identifier)
            val now = System.currentTimeMillis()
            
            val entity = StickerPackEntity(
                identifier = identifier,
                name = pack.name,
                publisher = pack.publisher,
                trayImageFile = pack.trayImageFile,
                isAnimated = pack.isAnimated,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                cloudId = existing?.cloudId,
                syncState = if (authManager?.isAuthenticated() == true) "PENDING" else "LOCAL_ONLY",
                lastSyncAt = existing?.lastSyncAt,
                visibility = existing?.visibility ?: "PRIVATE"
            )
            
            packDao.insert(entity)
            stickerDao.deleteByPackId(entity.identifier)
            
            pack.stickers.forEachIndexed { index, sticker ->
                val stickerEntity = StickerEntity(
                    id = Uuid.random().toString(),
                    packId = entity.identifier,
                    imageFile = sticker.imageFile,
                    sourceImageFile = sticker.sourceImageFile,
                    emojis = Json.encodeToString(sticker.emojis),
                    accessibilityText = sticker.accessibilityText,
                    decorationsJson = Json.encodeToString(sticker.decorations),
                    isAnimated = sticker.isAnimated,
                    sourceVideoFile = sticker.sourceVideoFile,
                    frameDecorationsJson = encodeFrameDecorations(sticker.frameDecorations),
                    sortOrder = index,
                    syncState = if (authManager?.isAuthenticated() == true) "PENDING" else "LOCAL_ONLY"
                )
                stickerDao.insert(stickerEntity)
            }
            
            // Enqueue cloud sync if authenticated
            if (authManager?.isAuthenticated() == true) {
                val syncOp = SyncOperation(
                    id = Uuid.random().toString(),
                    type = if (existing != null) SyncOperationType.UPDATE_PACK else SyncOperationType.CREATE_PACK,
                    targetId = identifier,
                    payload = createPackPayload(pack),
                    status = SyncOperationStatus.PENDING,
                    createdAt = now
                )
                syncManager?.enqueue(syncOp)
            }
        }
    }
    
    override suspend fun deletePack(identifier: String) {
        withContext(Dispatchers.IO) {
            val pack = packDao.getById(identifier) ?: return@withContext
            
            // Local delete
            val stickers = stickerDao.getByPackId(identifier)
            stickers.forEach { sticker ->
                listOfNotNull(sticker.imageFile, sticker.sourceImageFile).distinct().forEach {
                    fileStorage.deleteImage(it)
                }
            }
            fileStorage.deleteImage(pack.trayImageFile)
            stickerDao.deleteByPackId(identifier)
            packDao.delete(pack)
            
            // Cloud delete if synced
            if (pack.cloudId != null && authManager?.isAuthenticated() == true) {
                val syncOp = SyncOperation(
                    id = Uuid.random().toString(),
                    type = SyncOperationType.DELETE_PACK,
                    targetId = pack.cloudId ?: identifier,
                    payload = "{}",
                    status = SyncOperationStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )
                syncManager?.enqueue(syncOp)
            }
        }
    }
    
    override suspend fun syncAll(): SyncReport {
        return syncManager?.sync() ?: SyncReport(result = SyncResult.SkippedNotAuthenticated)
    }
    
    override suspend fun syncPack(packId: String): SyncReport {
        // Trigger sync for specific pack
        return syncManager?.sync() ?: SyncReport(result = SyncResult.SkippedNotAuthenticated)
    }
    
    override fun observeSyncStatus(): Flow<SyncStatus> {
        return syncManager?.operationsFlow?.map { operations ->
            when {
                operations.any { it.status == SyncOperationStatus.IN_PROGRESS } -> SyncStatus.SYNCING
                operations.any { it.status == SyncOperationStatus.FAILED } -> SyncStatus.FAILED
                operations.any { it.status == SyncOperationStatus.PENDING } -> SyncStatus.PENDING
                else -> SyncStatus.IDLE
            }
        } ?: flowOf(SyncStatus.IDLE)
    }
    
    override suspend fun getPendingSyncCount(): Int {
        return syncManager?.operationsFlow?.first()?.count { 
            it.status == SyncOperationStatus.PENDING 
        } ?: 0
    }
    
    private fun createPackPayload(pack: StickerPack): String {
        val request = CreateStickerPackRequest(
            name = pack.name,
            description = null,
            visibility = "PRIVATE",
            stickers = pack.stickers.mapIndexed { index, sticker ->
                StickerPackStickerInput(
                    name = "sticker_$index",
                    filename = sticker.imageFile.substringAfterLast("/"),
                    url = sticker.imageFile,
                    order = index
                )
            }
        )
        return Json.encodeToString(request)
    }
}
```

Note: Need to add imports for `SyncOperation`, `SyncOperationStatus`, `SyncOperationType`, `SyncReport`, `SyncResult`, `CreateStickerPackRequest`, `StickerPackStickerInput`, etc.

- [ ] **Step 2: Commit**

```bash
git add data/repository/StickerRepositoryImpl.kt
git commit -m "feat: integrate cloud sync into StickerRepositoryImpl"
```

---

## Phase 5: UI Implementation

### Task 5.1: Create LoginScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/presentation/auth/LoginState.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/auth/LoginIntent.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/auth/LoginViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/auth/LoginScreen.kt`

- [ ] **Step 1: Create LoginState**

Create `presentation/auth/LoginState.kt`:

```kotlin
package presentation.auth

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmailValid: Boolean = true,
    val isPasswordValid: Boolean = true
)
```

- [ ] **Step 2: Create LoginIntent**

Create `presentation/auth/LoginIntent.kt`:

```kotlin
package presentation.auth

sealed class LoginIntent {
    data class UpdateEmail(val email: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data object Submit : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object DismissError : LoginIntent()
}
```

- [ ] **Step 3: Create LoginViewModel**

Create `presentation/auth/LoginViewModel.kt`:

```kotlin
package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.model.LoginRequest
import data.auth.model.toDomainModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authApiService: AuthApiService,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    private val _effect = MutableStateFlow<LoginEffect?>(null)
    val effect: StateFlow<LoginEffect?> = _effect
    
    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> {
                _state.update { it.copy(email = intent.email, isEmailValid = true, error = null) }
            }
            is LoginIntent.UpdatePassword -> {
                _state.update { it.copy(password = intent.password, isPasswordValid = true, error = null) }
            }
            is LoginIntent.Submit -> login()
            is LoginIntent.NavigateToRegister -> {
                _effect.value = LoginEffect.NavigateToRegister
            }
            is LoginIntent.DismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }
    
    private fun login() {
        val currentState = _state.value
        
        // Validation
        val isEmailValid = currentState.email.contains("@")
        val isPasswordValid = currentState.password.length >= 6
        
        if (!isEmailValid || !isPasswordValid) {
            _state.update { 
                it.copy(
                    isEmailValid = isEmailValid,
                    isPasswordValid = isPasswordValid
                )
            }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val response = authApiService.login(
                    LoginRequest(
                        email = currentState.email,
                        password = currentState.password
                    )
                )
                
                response.data?.let { authData ->
                    authManager.saveTokens(
                        accessToken = authData.tokens.accessToken,
                        refreshToken = authData.tokens.refreshToken,
                        expiresIn = authData.tokens.expiresIn
                    )
                    authManager.saveUser(authData.user.toDomainModel())
                    _effect.value = LoginEffect.NavigateToHome
                } ?: run {
                    _state.update { it.copy(isLoading = false, error = "Login failed") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
            }
        }
    }
    
    fun clearEffect() {
        _effect.value = null
    }
}

sealed class LoginEffect {
    data object NavigateToHome : LoginEffect()
    data object NavigateToRegister : LoginEffect()
}
```

- [ ] **Step 4: Create LoginScreen**

Create `presentation/auth/LoginScreen.kt`:

```kotlin
package presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import presentation.components.AppButton
import presentation.components.AppTextField
import presentation.theme.neubrutalScreenBackground

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val effect by viewModel.effect.collectAsState()
    
    LaunchedEffect(effect) {
        when (effect) {
            is LoginEffect.NavigateToHome -> {
                viewModel.clearEffect()
                onNavigateToHome()
            }
            is LoginEffect.NavigateToRegister -> {
                viewModel.clearEffect()
                onNavigateToRegister()
            }
            null -> {}
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        AppTextField(
            value = state.email,
            onValueChange = { viewModel.onIntent(LoginIntent.UpdateEmail(it)) },
            label = "Email",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = !state.isEmailValid,
            supportingText = if (!state.isEmailValid) "Invalid email" else null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AppTextField(
            value = state.password,
            onValueChange = { viewModel.onIntent(LoginIntent.UpdatePassword(it)) },
            label = "Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            isError = !state.isPasswordValid,
            supportingText = if (!state.isPasswordValid) "Min 6 characters" else null
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        AppButton(
            text = "Login",
            onClick = { viewModel.onIntent(LoginIntent.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { viewModel.onIntent(LoginIntent.NavigateToRegister) }) {
            Text("Don't have an account? Register")
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add presentation/auth/LoginState.kt presentation/auth/LoginIntent.kt presentation/auth/LoginViewModel.kt presentation/auth/LoginScreen.kt
git commit -m "feat: add LoginScreen with ViewModel, state, and intent"
```

---

### Task 5.2: Create RegisterScreen

**Files:**
- Create: `presentation/auth/RegisterScreen.kt`
- Create: `presentation/auth/RegisterViewModel.kt`

- [ ] **Step 1: Create RegisterViewModel**

Create `presentation/auth/RegisterViewModel.kt`:

```kotlin
package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.model.RegisterRequest
import data.auth.model.toDomainModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class RegisterIntent {
    data class UpdateName(val name: String) : RegisterIntent()
    data class UpdateUsername(val username: String) : RegisterIntent()
    data class UpdateEmail(val email: String) : RegisterIntent()
    data class UpdatePassword(val password: String) : RegisterIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    data object Submit : RegisterIntent()
    data object NavigateToLogin : RegisterIntent()
}

sealed class RegisterEffect {
    data object NavigateToHome : RegisterEffect()
    data object NavigateToLogin : RegisterEffect()
}

class RegisterViewModel(
    private val authApiService: AuthApiService,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()
    
    private val _effect = MutableStateFlow<RegisterEffect?>(null)
    val effect: StateFlow<RegisterEffect?> = _effect
    
    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateName -> _state.update { it.copy(name = intent.name) }
            is RegisterIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is RegisterIntent.UpdateEmail -> _state.update { it.copy(email = intent.email) }
            is RegisterIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is RegisterIntent.UpdateConfirmPassword -> _state.update { it.copy(confirmPassword = intent.confirmPassword) }
            is RegisterIntent.Submit -> register()
            is RegisterIntent.NavigateToLogin -> _effect.value = RegisterEffect.NavigateToLogin
        }
    }
    
    private fun register() {
        val currentState = _state.value
        
        if (currentState.password != currentState.confirmPassword) {
            _state.update { it.copy(error = "Passwords don't match") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val response = authApiService.register(
                    RegisterRequest(
                        email = currentState.email,
                        username = currentState.username,
                        password = currentState.password,
                        name = currentState.name.takeIf { it.isNotBlank() }
                    )
                )
                
                response.data?.let { authData ->
                    authManager.saveTokens(
                        accessToken = authData.tokens.accessToken,
                        refreshToken = authData.tokens.refreshToken,
                        expiresIn = authData.tokens.expiresIn
                    )
                    authManager.saveUser(authData.user.toDomainModel())
                    _effect.value = RegisterEffect.NavigateToHome
                } ?: run {
                    _state.update { it.copy(isLoading = false, error = "Registration failed") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Registration failed") }
            }
        }
    }
    
    fun clearEffect() {
        _effect.value = null
    }
}
```

- [ ] **Step 2: Create RegisterScreen**

Create `presentation/auth/RegisterScreen.kt`:

```kotlin
package presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import presentation.components.AppButton
import presentation.components.AppTextField

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val effect by viewModel.effect.collectAsState()
    
    LaunchedEffect(effect) {
        when (effect) {
            is RegisterEffect.NavigateToHome -> {
                viewModel.clearEffect()
                onNavigateToHome()
            }
            is RegisterEffect.NavigateToLogin -> {
                viewModel.clearEffect()
                onNavigateToLogin()
            }
            null -> {}
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        AppTextField(
            value = state.name,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdateName(it)) },
            label = "Name (Optional)",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AppTextField(
            value = state.username,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdateUsername(it)) },
            label = "Username",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AppTextField(
            value = state.email,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdateEmail(it)) },
            label = "Email",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AppTextField(
            value = state.password,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdatePassword(it)) },
            label = "Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        AppTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdateConfirmPassword(it)) },
            label = "Confirm Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        AppButton(
            text = "Register",
            onClick = { viewModel.onIntent(RegisterIntent.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { viewModel.onIntent(RegisterIntent.NavigateToLogin) }) {
            Text("Already have an account? Login")
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add presentation/auth/RegisterScreen.kt presentation/auth/RegisterViewModel.kt
git commit -m "feat: add RegisterScreen with ViewModel"
```

---

### Task 5.3: Create ProfileScreen

**Files:**
- Create: `presentation/auth/ProfileScreen.kt`
- Create: `presentation/auth/ProfileViewModel.kt`

- [ ] **Step 1: Create ProfileViewModel**

Create `presentation/auth/ProfileViewModel.kt`:

```kotlin
package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init {
        loadUser()
    }
    
    private fun loadUser() {
        viewModelScope.launch {
            val user = authManager.getUser()
            _state.value = ProfileState(user = user, isLoading = false)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authManager.clearTokens()
            _state.value = _state.value.copy(user = null)
        }
    }
}

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = true
)
```

- [ ] **Step 2: Create ProfileScreen**

Create `presentation/auth/ProfileScreen.kt`:

```kotlin
package presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import presentation.components.AppButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar placeholder
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        state.user?.let { user ->
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (user.name != null) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AppButton(
                text = "Logout",
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth()
            )
        } ?: run {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Not logged in")
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add presentation/auth/ProfileScreen.kt presentation/auth/ProfileViewModel.kt
git commit -m "feat: add ProfileScreen with ViewModel and logout"
```

---

### Task 5.4: Create SyncScreen

**Files:**
- Create: `presentation/sync/SyncState.kt`
- Create: `presentation/sync/SyncIntent.kt`
- Create: `presentation/sync/SyncViewModel.kt`
- Create: `presentation/sync/SyncScreen.kt`

- [ ] **Step 1: Create SyncState and SyncIntent**

Create `presentation/sync/SyncState.kt`:

```kotlin
package presentation.sync

import domain.model.SyncOperation
import domain.model.SyncReport

data class SyncState(
    val operations: List<SyncOperation> = emptyList(),
    val isSyncing: Boolean = false,
    val lastReport: SyncReport? = null,
    val isLoading: Boolean = true
)
```

Create `presentation/sync/SyncIntent.kt`:

```kotlin
package presentation.sync

sealed class SyncIntent {
    data object LoadOperations : SyncIntent()
    data object SyncNow : SyncIntent()
    data class RetryOperation(val operationId: String) : SyncIntent()
    data class CancelOperation(val operationId: String) : SyncIntent()
    data object ClearCompleted : SyncIntent()
}
```

- [ ] **Step 2: Create SyncViewModel**

Create `presentation/sync/SyncViewModel.kt`:

```kotlin
package presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.sync.SyncManager
import domain.model.SyncOperationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncViewModel(
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()
    
    init {
        observeSyncState()
    }
    
    private fun observeSyncState() {
        viewModelScope.launch {
            syncManager.operationsFlow.collect { operations ->
                _state.update { 
                    it.copy(
                        operations = operations,
                        isLoading = false
                    )
                }
            }
        }
        
        viewModelScope.launch {
            syncManager.isSyncing.collect { isSyncing ->
                _state.update { it.copy(isSyncing = isSyncing) }
            }
        }
        
        viewModelScope.launch {
            syncManager.lastSyncReport.collect { report ->
                _state.update { it.copy(lastReport = report) }
            }
        }
    }
    
    fun onIntent(intent: SyncIntent) {
        when (intent) {
            is SyncIntent.LoadOperations -> { /* Already observing */ }
            is SyncIntent.SyncNow -> syncNow()
            is SyncIntent.RetryOperation -> retryOperation(intent.operationId)
            is SyncIntent.CancelOperation -> cancelOperation(intent.operationId)
            is SyncIntent.ClearCompleted -> clearCompleted()
        }
    }
    
    private fun syncNow() {
        viewModelScope.launch {
            syncManager.sync()
        }
    }
    
    private fun retryOperation(operationId: String) {
        viewModelScope.launch {
            syncManager.retry(operationId)
        }
    }
    
    private fun cancelOperation(operationId: String) {
        viewModelScope.launch {
            syncManager.cancel(operationId)
        }
    }
    
    private fun clearCompleted() {
        viewModelScope.launch {
            syncManager.clearCompleted()
        }
    }
}
```

- [ ] **Step 3: Create SyncScreen**

Create `presentation/sync/SyncScreen.kt`:

```kotlin
package presentation.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import presentation.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: SyncViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Sync Status",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Status Card
            SyncStatusCard(
                isSyncing = state.isSyncing,
                lastReport = state.lastReport,
                pendingCount = state.operations.count { 
                    it.status == SyncOperationStatus.PENDING 
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.onIntent(SyncIntent.SyncNow) },
                    enabled = !state.isSyncing
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now")
                }
                
                OutlinedButton(
                    onClick = { viewModel.onIntent(SyncIntent.ClearCompleted) }
                ) {
                    Text("Clear Done")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Operations List
            Text(
                text = "Operations (${state.operations.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.operations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sync operations", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.operations) { operation ->
                        SyncOperationItem(
                            operation = operation,
                            onRetry = { viewModel.onIntent(SyncIntent.RetryOperation(operation.id)) },
                            onCancel = { viewModel.onIntent(SyncIntent.CancelOperation(operation.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    isSyncing: Boolean,
    lastReport: SyncReport?,
    pendingCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (pendingCount > 0) 
                            Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (pendingCount > 0) 
                            MaterialTheme.colorScheme.warning else MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = if (isSyncing) "Syncing..." else if (pendingCount > 0) 
                            "$pendingCount pending" else "Up to date",
                        style = MaterialTheme.typography.titleMedium
                    )
                    lastReport?.let {
                        Text(
                            text = "Last sync: ${formatTimestamp(it.timestamp)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncOperationItem(
    operation: SyncOperation,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${operation.type.name}: ${operation.targetId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Status: ${operation.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (operation.status) {
                            SyncOperationStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            SyncOperationStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                            SyncOperationStatus.SUCCESS -> MaterialTheme.colorScheme.success
                            SyncOperationStatus.FAILED -> MaterialTheme.colorScheme.error
                            SyncOperationStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    operation.errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                when (operation.status) {
                    SyncOperationStatus.FAILED -> {
                        Row {
                            TextButton(onClick = onRetry) {
                                Text("Retry")
                            }
                            TextButton(onClick = onCancel) {
                                Text("Remove")
                            }
                        }
                    }
                    SyncOperationStatus.PENDING -> {
                        TextButton(onClick = onCancel) {
                            Text("Cancel")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    // Simple relative time formatting
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
```

Note: Need to add proper imports for Icons.Default.Warning, CheckCircle, etc.

- [ ] **Step 4: Commit**

```bash
git add presentation/sync/SyncState.kt presentation/sync/SyncIntent.kt presentation/sync/SyncViewModel.kt presentation/sync/SyncScreen.kt
git commit -m "feat: add SyncScreen with operation list, retry, and manual sync"
```

---

### Task 5.5: Update HomeScreen with Bottom Bar

**Files:**
- Modify: `presentation/home/HomeScreen.kt`
- Modify: `presentation/home/HomeState.kt`
- Modify: `presentation/home/HomeIntent.kt`
- Modify: `presentation/home/HomeViewModel.kt`

- [ ] **Step 1: Update HomeState**

Open `presentation/home/HomeState.kt`:

```kotlin
package presentation.home

import domain.model.StickerPack
import domain.model.User

data class HomeState(
    val isLoading: Boolean = false,
    val packs: List<StickerPack> = emptyList(),
    val error: String? = null,
    val currentUser: User? = null,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0
)
```

- [ ] **Step 2: Update HomeIntent**

Open `presentation/home/HomeIntent.kt`:

```kotlin
package presentation.home

sealed class HomeIntent {
    data object LoadPacks : HomeIntent()
    data class DeletePack(val packId: String) : HomeIntent()
    data class AddToWhatsApp(val packId: String) : HomeIntent()
    data object CreateNewPack : HomeIntent()
    data object NavigateToProfile : HomeIntent()
    data object NavigateToSync : HomeIntent()
    data object RefreshSync : HomeIntent()
}
```

- [ ] **Step 3: Update HomeViewModel**

Open `presentation/home/HomeViewModel.kt`:

```kotlin
package presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.sync.SyncManager
import domain.model.StickerPack
import domain.model.SyncOperationStatus
import domain.repository.StickerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_add_pack
import setiker.composeapp.generated.resources.error_failed_delete_pack
import setiker.composeapp.generated.resources.error_pack_min_stickers_whatsapp
import setiker.composeapp.generated.resources.success_pack_added_whatsapp

class HomeViewModel(
    private val repository: StickerRepository,
    private val authManager: AuthManager? = null,
    private val syncManager: SyncManager? = null
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeAuthState()
        observeSyncState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager?.currentUser?.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncManager?.isSyncing?.collect { isSyncing ->
                _state.update { it.copy(isSyncing = isSyncing) }
            }
        }
        
        viewModelScope.launch {
            syncManager?.operationsFlow?.collect { operations ->
                val pendingCount = operations.count { 
                    it.status == SyncOperationStatus.PENDING 
                }
                _state.update { it.copy(pendingSyncCount = pendingCount) }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadPacks -> loadPacks()
            is HomeIntent.DeletePack -> deletePack(intent.packId)
            is HomeIntent.AddToWhatsApp -> addToWhatsApp(intent.packId)
            is HomeIntent.CreateNewPack -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToCreatePack)
                }
            }
            is HomeIntent.NavigateToProfile -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToProfile)
                }
            }
            is HomeIntent.NavigateToSync -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToSync)
                }
            }
            is HomeIntent.RefreshSync -> refreshSync()
        }
    }

    private fun loadPacks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val packs = repository.getAllPacks()
                _state.update { it.copy(isLoading = false, packs = packs) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun refreshSync() {
        viewModelScope.launch {
            syncManager?.sync()
        }
    }

    // ... rest of existing methods (deletePack, addToWhatsApp) remain the same
}

// Add new effects
sealed class HomeEffect {
    // ... existing effects
    data object NavigateToProfile : HomeEffect()
    data object NavigateToSync : HomeEffect()
}
```

Note: Need to add the new effects to the existing HomeEffect sealed class.

- [ ] **Step 4: Update HomeScreen with bottom bar**

Open `presentation/home/HomeScreen.kt` and modify:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onPackClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onIntent(HomeIntent.LoadPacks)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.my_stickers_title),
                actions = {
                    // Show sync indicator if syncing
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        },
        bottomBar = {
            HomeBottomBar(
                currentUser = state.currentUser,
                pendingSyncCount = state.pendingSyncCount,
                isSyncing = state.isSyncing,
                onProfileClick = { onIntent(HomeIntent.NavigateToProfile) },
                onSyncClick = { onIntent(HomeIntent.NavigateToSync) },
                onAddPackClick = { onIntent(HomeIntent.CreateNewPack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground(),
        floatingActionButton = {
            // Remove FAB from here - it's now in bottom bar
        }
    ) { innerPadding ->
        // ... rest of content remains the same
        // Add pull-to-refresh
        androidx.compose.material.pullrefresh.pullRefresh(
            // ... implement pull to refresh
        ) {
            // existing content
        }
    }
}
```

- [ ] **Step 5: Create HomeBottomBar component**

Create `presentation/components/HomeBottomBar.kt`:

```kotlin
package presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBottomBar(
    currentUser: User?,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onProfileClick: () -> Unit,
    onSyncClick: () -> Unit,
    onAddPackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        actions = {
            // Profile button
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = if (currentUser != null) "Profile" else "Login"
                )
            }
            
            // Sync button with badge
            BadgedBox(
                badge = {
                    if (pendingSyncCount > 0 && !isSyncing) {
                        Badge {
                            Text(pendingSyncCount.toString())
                        }
                    }
                }
            ) {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync"
                    )
                }
            }
        },
        floatingActionButton = {
            PackBottomBarFab(
                icon = Icons.Default.Add,
                contentDescription = "Create Pack",
                onClick = onAddPackClick
            )
        }
    )
}
```

- [ ] **Step 6: Commit**

```bash
git add presentation/home/HomeScreen.kt presentation/home/HomeState.kt presentation/home/HomeIntent.kt presentation/home/HomeViewModel.kt presentation/components/HomeBottomBar.kt
git commit -m "feat: update HomeScreen with bottom bar, sync indicators, and auth state"
```

---

## Phase 6: Navigation & DI

### Task 6.1: Update Navigation

**Files:**
- Modify: `presentation/navigation/Screen.kt`
- Modify: `presentation/navigation/AppNavigation.kt`

- [ ] **Step 1: Add new screens to Screen sealed class**

Open `presentation/navigation/Screen.kt`:

```kotlin
package presentation.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object Home : Screen()
    
    @Serializable
    data class PackDetail(val packId: String) : Screen()
    
    @Serializable
    data class CreatePack(val packId: String? = null) : Screen()
    
    @Serializable
    data class Editor(val stickerIndex: Int? = null, val packId: String) : Screen()
    
    @Serializable
    data class Crop(val imagePath: String) : Screen()

    @Serializable
    data class VideoTrim(val videoPath: String) : Screen()

    @Serializable
    data class VideoCrop(
        val videoPath: String,
        val packId: String,
        val trimStartMs: Long,
        val trimEndMs: Long,
        val fps: Int,
        val speed: Float
    ) : Screen()

    @Serializable
    data class AnimatedEditor(val draftId: String, val packId: String) : Screen()
    
    // New auth screens
    @Serializable
    data object Login : Screen()
    
    @Serializable
    data object Register : Screen()
    
    @Serializable
    data object Profile : Screen()
    
    @Serializable
    data object Sync : Screen()
}
```

- [ ] **Step 2: Update AppNavigation**

Open `presentation/navigation/AppNavigation.kt`:

```kotlin
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onAddToWhatsApp: ((String, String) -> Unit)? = null
) {
    // ... existing state declarations ...

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ... existing composables ...
        
        // Add new screens
        composable("login") {
            LoginScreen(
                viewModel = koinViewModel(),
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        
        composable("register") {
            RegisterScreen(
                viewModel = koinViewModel(),
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }
        
        composable("profile") {
            ProfileScreen(
                viewModel = koinViewModel(),
                onLogout = {
                    navController.navigate("home") {
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }
        
        composable("sync") {
            SyncScreen(
                viewModel = koinViewModel(),
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
```

Note: Need to add `import org.koin.compose.viewmodel.koinViewModel` and other necessary imports.

- [ ] **Step 3: Update HomeScreenRoot navigation**

Make sure HomeScreenRoot navigates to new screens:

```kotlin
// In HomeScreenRoot or wherever HomeScreen is called
HomeScreenRoot(
    onPackClick = { packId ->
        navController.navigate("packDetail/$packId")
    },
    onCreatePackClick = {
        navController.navigate("createPack")
    },
    onNavigateToProfile = {
        navController.navigate("profile")
    },
    onNavigateToSync = {
        navController.navigate("sync")
    }
)
```

- [ ] **Step 4: Commit**

```bash
git add presentation/navigation/Screen.kt presentation/navigation/AppNavigation.kt
git commit -m "feat: add Login, Register, Profile, and Sync screens to navigation"
```

---

### Task 6.2: Update DI Module

**Files:**
- Modify: `di/AppModule.kt`
- Modify: `di/AppModule.android.kt`
- Modify: `di/AppModule.ios.kt`

- [ ] **Step 1: Update common AppModule**

Open `di/AppModule.kt`:

```kotlin
package di

import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.AuthManagerImpl
import data.local.database.SyncOperationDao
import data.remote.CloudStickerRepository
import data.sync.NetworkMonitor
import data.sync.SyncManager
import data.sync.SyncManagerImpl
import data.sync.SyncWorker
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import presentation.auth.LoginViewModel
import presentation.auth.ProfileViewModel
import presentation.auth.RegisterViewModel
import presentation.sync.SyncViewModel
// ... existing imports

val appModule = module {
    includes(platformModule())

    // Repository
    singleOf(::StickerRepositoryImpl) bind StickerRepository::class
    single { SetikerApiService(authManager = get()) }
    single { StickerApiRepository(api = get(), fileStorage = get()) }
    single { AnimatedStickerDraftStore() }
    
    // Auth
    single<AuthManager> { AuthManagerImpl(dataStore = get()) }
    single { AuthApiService() }
    
    // Cloud
    single { CloudStickerRepository(authManager = get()) }
    
    // Sync
    single { SyncOperationDao() } // This needs to come from database
    single<SyncManager> { 
        SyncManagerImpl(
            operationDao = get(),
            cloudRepo = get(),
            authManager = get(),
            networkMonitor = get()
        )
    }
    single { SyncWorker(context = get()) }
    
    // Network
    single { NetworkMonitor(context = get()) }

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::PackDetailViewModel)
    viewModelOf(::CreatePackViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::CropViewModel)
    viewModelOf(::VideoTrimViewModel)
    viewModelOf(::VideoCropViewModel)
    viewModelOf(::AnimatedEditorViewModel)
    
    // Auth ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SyncViewModel)
}
```

Note: Need to add proper DataStore dependency and fix the SyncOperationDao injection (it should come from the Room database).

- [ ] **Step 2: Update Android DI**

Open `di/AppModule.android.kt`:

```kotlin
package di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import data.local.database.StickerDatabase
import data.sync.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            StickerDatabase::class.java,
            StickerDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigrationFrom(4) // Handle migration from v4 to v5
            .build()
    }
    
    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single { get<StickerDatabase>().syncOperationDao() }
    
    // DataStore
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("auth_prefs")
        }
    }
    
    // NetworkMonitor
    single { NetworkMonitor(androidContext()) }
}
```

- [ ] **Step 3: Update iOS DI**

Open `di/AppModule.ios.kt`:

```kotlin
package di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import data.local.database.StickerDatabase
import data.sync.NetworkMonitor
import kotlinx.cinterop.ObjCClassOf
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun platformModule() = module {
    // Database - iOS uses SQLDelight or Room via KMP
    // This depends on your current setup
    single { 
        // Your existing database initialization
        StickerDatabase.getInstance()
    }
    
    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single { get<StickerDatabase>().syncOperationDao() }
    
    // DataStore
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            NSFileManager.defaultManager()
                .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
                .first() as NSURL
                .path?.let { path ->
                    val datastoreFile = "$path/auth_prefs.preferences_pb"
                    // Create the file if needed
                    datastoreFile.toPath().toFile()
                } ?: throw IllegalStateException("Could not create DataStore")
        }
    }
    
    // NetworkMonitor
    single { NetworkMonitor() }
}
```

Note: The iOS DataStore setup might need adjustment based on your exact KMP setup.

- [ ] **Step 4: Commit**

```bash
git add di/AppModule.kt di/AppModule.android.kt di/AppModule.ios.kt
git commit -m "feat: update DI modules with auth, cloud, and sync dependencies"
```

---

## Phase 7: Testing & Polish

### Task 7.1: Add Unit Tests for AuthManager

**Files:**
- Create: `composeApp/src/commonTest/kotlin/data/auth/AuthManagerTest.kt`

- [ ] **Step 1: Create AuthManager tests**

Create `data/auth/AuthManagerTest.kt`:

```kotlin
package data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthManagerTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var authManager: AuthManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @BeforeTest
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope
        ) {
            "test_auth_prefs.preferences_pb".toPath()
        }
        authManager = AuthManagerImpl(dataStore)
    }
    
    @Test
    fun `saveTokens stores access and refresh tokens`() = testScope.runTest {
        authManager.saveTokens("access123", "refresh456", 3600)
        
        assertEquals("access123", authManager.getAccessToken())
        assertEquals("refresh456", authManager.getRefreshToken())
    }
    
    @Test
    fun `isAuthenticated returns true when token is valid`() = testScope.runTest {
        authManager.saveTokens("access123", "refresh456", 3600)
        
        assertTrue(authManager.isAuthenticated())
    }
    
    @Test
    fun `isAuthenticated returns false when token is expired`() = testScope.runTest {
        // Save token that expired 1 hour ago
        authManager.saveTokens("access123", "refresh456", -3600)
        
        assertFalse(authManager.isAuthenticated())
    }
    
    @Test
    fun `clearTokens removes all auth data`() = testScope.runTest {
        authManager.saveTokens("access123", "refresh456", 3600)
        authManager.clearTokens()
        
        assertNull(authManager.getAccessToken())
        assertNull(authManager.getRefreshToken())
        assertFalse(authManager.isAuthenticated())
    }
    
    @Test
    fun `saveUser stores user data`() = testScope.runTest {
        val user = User(
            id = "1",
            email = "test@example.com",
            username = "testuser",
            name = "Test User",
            role = UserRole("1", "user"),
            isActive = true,
            createdAt = 1234567890
        )
        
        authManager.saveUser(user)
        val storedUser = authManager.getUser()
        
        assertNotNull(storedUser)
        assertEquals(user.id, storedUser.id)
        assertEquals(user.email, storedUser.email)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add data/auth/AuthManagerTest.kt
git commit -m "test: add AuthManager unit tests"
```

---

### Task 7.2: Add Tests for SyncManager

**Files:**
- Create: `composeApp/src/commonTest/kotlin/data/sync/SyncManagerTest.kt`

- [ ] **Step 1: Create SyncManager tests**

Create `data/sync/SyncManagerTest.kt`:

```kotlin
package data.sync

import data.auth.AuthManager
import data.local.database.SyncOperationDao
import data.remote.CloudStickerRepository
import domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncManagerTest {
    private lateinit var syncManager: SyncManager
    private lateinit var mockDao: MockSyncOperationDao
    private lateinit var mockCloudRepo: MockCloudStickerRepository
    private lateinit var mockAuthManager: MockAuthManager
    private lateinit var mockNetworkMonitor: MockNetworkMonitor
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @BeforeTest
    fun setup() {
        mockDao = MockSyncOperationDao()
        mockCloudRepo = MockCloudStickerRepository()
        mockAuthManager = MockAuthManager()
        mockNetworkMonitor = MockNetworkMonitor()
        
        syncManager = SyncManagerImpl(
            operationDao = mockDao,
            cloudRepo = mockCloudRepo,
            authManager = mockAuthManager,
            networkMonitor = mockNetworkMonitor,
            coroutineScope = testScope
        )
    }
    
    @Test
    fun `enqueue adds operation to queue`() = testScope.runTest {
        val operation = createTestOperation()
        syncManager.enqueue(operation)
        
        val operations = syncManager.operationsFlow.first()
        assertEquals(1, operations.size)
        assertEquals(operation.id, operations[0].id)
    }
    
    @Test
    fun `processQueue executes pending operations`() = testScope.runTest {
        mockAuthManager.setAuthenticated(true)
        mockNetworkMonitor.setOnline(true)
        
        val operation = createTestOperation(SyncOperationType.CREATE_PACK)
        syncManager.enqueue(operation)
        
        val report = syncManager.processQueue()
        
        assertTrue(report.result is SyncResult.Success)
        assertEquals(1, report.operationsProcessed)
        assertEquals(1, report.operationsSucceeded)
    }
    
    @Test
    fun `processQueue skips when offline`() = testScope.runTest {
        mockAuthManager.setAuthenticated(true)
        mockNetworkMonitor.setOnline(false)
        
        val report = syncManager.processQueue()
        
        assertTrue(report.result is SyncResult.SkippedOffline)
    }
    
    @Test
    fun `processQueue skips when not authenticated`() = testScope.runTest {
        mockAuthManager.setAuthenticated(false)
        mockNetworkMonitor.setOnline(true)
        
        val report = syncManager.processQueue()
        
        assertTrue(report.result is SyncResult.SkippedNotAuthenticated)
    }
    
    private fun createTestOperation(
        type: SyncOperationType = SyncOperationType.CREATE_PACK
    ): SyncOperation {
        return SyncOperation(
            id = "test-op-1",
            type = type,
            targetId = "pack-1",
            payload = "{}",
            status = SyncOperationStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
    }
}

// Mock classes would be implemented here or in separate files
```

- [ ] **Step 2: Commit**

```bash
git add data/sync/SyncManagerTest.kt
git commit -m "test: add SyncManager unit tests"
```

---

### Task 7.3: Final Integration Testing

**Files:**
- Create: `composeApp/src/commonTest/kotlin/data/repository/StickerRepositorySyncTest.kt`

- [ ] **Step 1: Create integration test**

Create `data/repository/StickerRepositorySyncTest.kt`:

```kotlin
package data.repository

import data.local.database.StickerPackDao
import data.local.database.StickerDao
import data.local.database.SyncOperationDao
import data.storage.StickerFileStorage
import data.sync.SyncManager
import domain.model.Sticker
import domain.model.StickerPack
import domain.model.SyncOperationStatus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StickerRepositorySyncTest {
    private lateinit var repository: StickerRepositoryImpl
    private lateinit var packDao: StickerPackDao
    private lateinit var stickerDao: StickerDao
    private lateinit var mockFileStorage: MockStickerFileStorage
    private lateinit var mockSyncManager: MockSyncManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @BeforeTest
    fun setup() {
        // Setup with in-memory database or mocks
        // This is a template - actual implementation depends on test infrastructure
    }
    
    @Test
    fun `savePack enqueues sync operation when authenticated`() = testScope.runTest {
        val pack = StickerPack(
            identifier = "",
            name = "Test Pack",
            publisher = "Test",
            trayImageFile = "",
            stickers = listOf(Sticker(imageFile = "test.png"))
        )
        
        repository.savePack(pack)
        
        val pendingOps = mockSyncManager.getPendingOperations()
        assertEquals(1, pendingOps.size)
        assertEquals(SyncOperationStatus.PENDING, pendingOps[0].status)
    }
    
    @Test
    fun `deletePack enqueues cloud delete when cloudId exists`() = testScope.runTest {
        // Save a pack with cloudId
        val pack = StickerPack(
            identifier = "pack-1",
            name = "Test Pack",
            publisher = "Test",
            trayImageFile = "",
            stickers = emptyList()
        )
        // ... setup pack with cloudId in DB
        
        repository.deletePack("pack-1")
        
        val pendingOps = mockSyncManager.getPendingOperations()
        val deleteOps = pendingOps.filter { it.type.name.contains("DELETE") }
        assertTrue(deleteOps.isNotEmpty())
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add data/repository/StickerRepositorySyncTest.kt
git commit -m "test: add repository sync integration tests"
```

---

### Task 7.4: Build Verification

- [ ] **Step 1: Run tests**

```bash
./gradlew composeApp:testDebugUnitTest
```

Expected: All tests pass (or existing tests still pass if new ones fail due to incomplete implementation)

- [ ] **Step 2: Check compilation**

```bash
./gradlew composeApp:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit any final fixes**

```bash
git commit -am "fix: resolve compilation issues and test failures"
```

---

## Summary

This implementation plan covers:
- **Phase 1:** Dependencies, domain models, database migration, AuthManager, NetworkMonitor
- **Phase 2:** Auth API service, cloud repository, auth interceptor
- **Phase 3:** SyncManager with queue processing, background workers for Android/iOS
- **Phase 4:** Enhanced StickerRepository with cloud sync integration
- **Phase 5:** LoginScreen, RegisterScreen, ProfileScreen, SyncScreen, HomeScreen updates
- **Phase 6:** Navigation updates, DI module updates
- **Phase 7:** Unit tests and integration tests

**Total estimated tasks:** 30+
**Estimated implementation time:** 8-12 hours for a skilled developer

**Next Steps:**
1. Execute tasks sequentially
2. Run tests after each phase
3. Commit frequently
4. Review with user after each major phase
