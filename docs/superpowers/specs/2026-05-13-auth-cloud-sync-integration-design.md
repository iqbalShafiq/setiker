# Auth & Cloud Sync Integration Design

**Date:** 2026-05-13
**Approach:** Unified Repository with Auto-Sync (Approach C)
**Status:** Approved

---

## 1. Overview

Integrate the Setiker mobile app (Kotlin Multiplatform) with the new WhatsApp Sticker API authentication and cloud storage system. The app must remain fully functional offline while transparently syncing local data to the cloud when online and authenticated.

**Key Requirements:**
- Offline-first: all operations succeed locally regardless of connectivity
- Auto-sync: no manual "Upload to Cloud" buttons; sync happens automatically
- Transparent: existing ViewModels require minimal changes
- Conflict resolution: server data is source of truth for cloud-synced items
- Timestamp standardization: all timestamps stored as UTC epoch millis

---

## 2. API Changes Summary

The API (`stiker-api`) has added the following major features:

### 2.1 Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login with JWT tokens
- `POST /api/v1/auth/logout` - Invalidate tokens
- `POST /api/v1/auth/refresh` - Refresh access token via refresh token cookie
- `GET /api/v1/auth/me` - Get current user profile
- `POST /api/v1/auth/change-password` - Change password

### 2.2 Sticker CRUD with Ownership
- All sticker operations now require Bearer JWT token
- Sticker visibility: `public`, `private`, `unlisted`
- Owner-based access control
- `GET /api/v1/stickers/{id}/access` - Check access level

### 2.3 Sharing System
- `POST /api/v1/stickers/{id}/share` - Share with user (view/full)
- `DELETE /api/v1/stickers/{id}/share/{shareId}` - Remove share
- `POST /api/v1/stickers/{id}/link` - Create shareable link
- `DELETE /api/v1/stickers/{id}/link/{linkId}` - Revoke link

### 2.4 Sticker Packs (Cloud)
- Full CRUD for cloud sticker packs
- Sharing and link sharing for packs
- Sticker reordering within packs
- `GET /api/v1/sticker-packs/public` - Browse public packs

### 2.5 Batch Upload
- `POST /api/v1/upload` - Upload up to 30 images to a pack

### 2.6 Offline Sync
- `GET /api/v1/sync?lastSyncAt=` - Delta sync for offline-first support

---

## 3. Architecture

### 3.1 High-Level Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ HomeScreen │  │PackDetail│  │LoginScreen│  │SyncScreen│   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
│       │             │             │             │           │
│       └─────────────┴──────┬──────┴─────────────┘           │
│                            │                                 │
│                    ┌───────▼────────┐                        │
│                    │   ViewModels    │                        │
│                    └───────┬────────┘                        │
└────────────────────────────┼────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────┐
│                      Domain Layer                            │
│                    ┌───────▼────────┐                        │
│                    │StickerRepository│ (Unified Interface)   │
│                    └───────┬────────┘                        │
│                            │                                 │
│                    ┌───────┴────────┐                        │
│                    │   SyncManager   │                        │
│                    └───────┬────────┘                        │
└────────────────────────────┼────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────┐
│                      Data Layer                              │
│         ┌──────────────────┼──────────────────┐             │
│         │                  │                  │             │
│    ┌────▼────┐       ┌────▼────┐       ┌────▼────┐        │
│    │Local DB │       │SyncQueue│       │Cloud API│        │
│    │(Room)   │       │(Room)   │       │(Ktor)   │        │
│    └─────────┘       └─────────┘       └─────────┘        │
│                                                             │
│    ┌──────────┐  ┌──────────┐  ┌──────────────┐          │
│    │AuthManager│  │DataStore │  │NetworkMonitor│          │
│    │(Tokens)  │  │(Settings)│  │(Online/Off)  │          │
│    └──────────┘  └──────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 New Components

| Component | Responsibility | Scope |
|-----------|---------------|-------|
| `AuthManager` | JWT token storage, refresh, validation | Singleton |
| `AuthApiService` | Auth endpoint calls (login/register/etc) | Per-request |
| `CloudStickerRepository` | API calls for sticker/pack CRUD | Per-request |
| `SyncManager` | Queue management, conflict resolution | Singleton |
| `SyncWorker` | Background sync execution | Platform-specific |
| `NetworkMonitor` | Connectivity state observation | Singleton |
| `LocalPendingOperationDao` | Persist pending sync operations | Room DAO |

---

## 4. Timestamp Standardization

**Rule:** All timestamps stored and compared as UTC epoch milliseconds (`Long`).

### 4.1 Server to Local
- API returns ISO 8601 strings (`createdAt`, `updatedAt`, `deletedAt`)
- Parse using ` kotlinx.datetime.Instant.parse()` → convert to epoch millis
- Store as `Long` in Room database

### 4.2 Local to Server
- Sync API accepts `lastSyncAt` as ISO 8601 string
- Convert epoch millis → `Instant.fromEpochMilliseconds()` → `toString()`

### 4.3 Conflict Resolution
```kotlin
// Last-write-wins based on updatedAt
typealias TimestampMs = Long

fun resolveConflict(localUpdatedAt: TimestampMs, serverUpdatedAt: TimestampMs): Source {
    return if (serverUpdatedAt >= localUpdatedAt) Source.SERVER else Source.LOCAL
}
```

---

## 5. Authentication

### 5.1 Token Storage
- **Android:** EncryptedSharedPreferences (via androidx.security:security-crypto)
- **iOS:** Keychain (via expect/actual)
- **KMP Common:** DataStore with expect/actual encryption layer

### 5.2 Token Lifecycle
```
Login/Register → Store accessToken + refreshToken
                ↓
        API Request → Add "Authorization: Bearer {accessToken}"
                ↓
        401 Response → Call /auth/refresh (refresh token from cookie)
                ↓
        Refresh Success → Update accessToken, retry original request
                ↓
        Refresh Failed → Clear tokens, emit AuthState.Unauthenticated
```

### 5.3 Auth State
```kotlin
sealed class AuthState {
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

### 5.4 Auto-Refresh
- Access token expiry tracked locally (from `expiresIn` field)
- Proactive refresh when token is within 5 minutes of expiry
- Ktor client plugin intercepts 401 and handles refresh transparently

---

## 6. Unified Repository Pattern

### 6.1 Interface Design

```kotlin
interface StickerRepository {
    // Existing local methods (unchanged signatures)
    suspend fun getAllPacks(): List<StickerPack>
    suspend fun getPack(identifier: String): StickerPack
    suspend fun savePack(pack: StickerPack)
    suspend fun deletePack(identifier: String)
    suspend fun addStickerToPack(packId: String, sticker: Sticker)
    suspend fun updateStickerInPack(packId: String, index: Int, sticker: Sticker)
    suspend fun removeStickerFromPack(packId: String, index: Int)
    
    // New cloud-aware methods
    suspend fun syncAll(): Result<SyncReport>
    suspend fun syncPack(packId: String): Result<Unit>
    fun observeSyncStatus(): Flow<SyncStatus>
}
```

### 6.2 Implementation Strategy

**Local Operations (Always Primary):**
```kotlin
override suspend fun savePack(pack: StickerPack) {
    // 1. Save to local DB (immediate success)
    localDao.savePack(pack.toEntity())
    
    // 2. Queue cloud operation if applicable
    if (authManager.isAuthenticated()) {
        syncManager.enqueue(SyncOperation(
            type = SyncOperationType.UPSERT_PACK,
            targetId = pack.identifier,
            payload = pack.toJson()
        ))
    }
}
```

**Read Operations:**
- Always read from local DB
- Local DB is kept in sync via `SyncManager` pulling from cloud
- No network calls on read path

### 6.3 Data Mapping

**Local ↔ Domain ↔ Cloud Models:**
```
StickerPackEntity (Room) ↔ StickerPack (Domain) ↔ ApiStickerPack (API DTO)
StickerEntity (Room)     ↔ Sticker (Domain)      ↔ ApiSticker (API DTO)
```

**Sync Metadata (new columns in local DB):**
- `cloudId: String?` - ID from cloud server
- `syncState: SyncState` - `SYNCED`, `PENDING`, `FAILED`
- `lastSyncAt: Long?` - Last successful sync timestamp
- `pendingOperationId: String?` - Reference to sync queue item

---

## 7. Sync Engine

### 7.1 Sync Queue Schema (Room Entity)

```kotlin
@Entity(tableName = "sync_queue")
data class PendingSyncOperation(
    @PrimaryKey val id: String,
    val type: SyncOperationType,
    val targetId: String,       // Local pack/sticker ID
    val payload: String,        // JSON serialized data
    val status: SyncStatus,     // PENDING, IN_PROGRESS, SUCCESS, FAILED
    val errorMessage: String?,
    val retryCount: Int,
    val createdAt: Long,        // Epoch millis UTC
    val completedAt: Long?,
    val priority: Int           // Higher = process first
)

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

enum class SyncStatus { PENDING, IN_PROGRESS, SUCCESS, FAILED, CANCELLED }
```

### 7.2 SyncManager

```kotlin
class SyncManager(
    private val operationDao: LocalPendingOperationDao,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val networkMonitor: NetworkMonitor,
    private val localRepo: StickerRepositoryImpl
) {
    // Hot flow of all operations for UI observation
    val operationsFlow: Flow<List<PendingSyncOperation>> = 
        operationDao.observeAll()
    
    // Current active operation
    val activeOperationFlow: MutableStateFlow<PendingSyncOperation?> = 
        MutableStateFlow(null)
    
    fun enqueue(operation: PendingSyncOperation)
    suspend fun processQueue(): SyncReport
    suspend fun retry(operationId: String)
    suspend fun cancel(operationId: String)
    suspend fun clearCompleted()
}
```

### 7.3 Background Sync Worker

**Trigger Conditions:**
1. **App Startup:** Check queue on app launch if authenticated
2. **Network Restored:** Observe `NetworkMonitor.isOnline` → true
3. **Periodic:** Every 15 minutes (if authenticated + online)
4. **Manual:** User pulls-to-refresh or taps "Sync Now"

**Platform Implementation:**
- **Android:** `WorkManager` with `NetworkType.CONNECTED` constraint
- **iOS:** `BGAppRefreshTask` + `BGProcessingTask` via `BackgroundTasks` framework
- **Common:** `expect fun scheduleSyncWork()` / `actual` implementations

### 7.4 Sync Algorithm

```kotlin
suspend fun performSync(): SyncReport {
    if (!authManager.isAuthenticated()) return SyncReport.Skipped.NotAuthenticated
    if (!networkMonitor.isOnline) return SyncReport.Skipped.Offline
    
    val report = SyncReport()
    
    // Phase 1: Push local changes (upload queue)
    val pendingOps = operationDao.getPending()
    for (op in pendingOps) {
        val result = executeOperation(op)
        when (result) {
            is Success -> markCompleted(op, result.cloudId)
            is RetryableError -> if (op.retryCount < 5) retryLater(op)
            is PermanentError -> markFailed(op, result.error)
        }
    }
    
    // Phase 2: Pull server changes (delta sync)
    val lastSync = settingsDataStore.getLastSyncAt()
    val delta = cloudRepo.sync(lastSync)
    
    // Phase 3: Merge (server wins for conflicts)
    for (packDelta in delta.stickerPacks) {
        mergePack(packDelta, resolution = Source.SERVER)
    }
    for (stickerDelta in delta.stickers) {
        mergeSticker(stickerDelta, resolution = Source.SERVER)
    }
    
    // Phase 4: Update last sync timestamp
    settingsDataStore.setLastSyncAt(now())
    
    return report
}
```

### 7.5 Retry Strategy

- **Exponential Backoff:** 2s, 4s, 8s, 16s, 32s (max 60s)
- **Max Retries:** 5 per operation
- **Retryable Errors:** Network timeout, 5xx server errors, no connectivity
- **Permanent Errors:** 4xx client errors (except 401), validation errors
- **Failed Operations:** Remain in queue with FAILED status, user can manually retry from SyncScreen

---

## 8. UI Design

### 8.1 HomeScreen Changes

**Bottom App Bar (using existing PackBottomBar component):**
```
┌─────────────────────────────────────────┐
│ [👤]  [🔄]        [➕ FAB]             │
│ Profile Sync       Add Pack             │
└─────────────────────────────────────────┘
```

- **Profile Icon (left):**
  - Unauthenticated → Navigate to LoginScreen
  - Authenticated → Show ProfileScreen (bottom sheet or navigate)
  
- **Sync Icon (middle):**
  - Badge with pending count if queue > 0
  - Animated spinner if actively syncing
  - Tap → Navigate to SyncScreen
  
- **FAB Add Pack (right):**
  - Existing ClayFab behavior (consistent with other screens)

**Pack Card Sync Indicators:**
- ✅ Green dot: Fully synced (`cloudId != null`, `syncState == SYNCED`)
- 🟡 Yellow dot: Pending sync (operations in queue for this pack)
- 🔴 Red dot: Sync failed (operations with FAILED status)
- No indicator: Local-only (never synced, `cloudId == null`)

### 8.2 PackDetailScreen Changes

**Top Bar:**
- Show sync status icon in trailing actions:
  - ✅ Synced / 🔄 Syncing / ❌ Failed / ☁️ Local-only
  - Tap icon → Bottom sheet with sync details (last sync time, queue status)

### 8.3 LoginScreen

**Layout:**
```
┌─────────────────────────────┐
│      [App Logo]             │
│                             │
│  Email: [____________]      │
│  Password: [____________]   │
│                             │
│  [      Login      ]        │
│                             │
│  Don't have account?        │
│  [    Register     ]        │
└─────────────────────────────┘
```

- Form validation: email format, password min 6 chars
- Loading state during API call
- Error display: inline or Snackbar
- Navigate to Home on success (token stored automatically)

### 8.4 RegisterScreen

**Layout:**
```
┌─────────────────────────────┐
│      [App Logo]             │
│                             │
│  Name: [____________]       │
│  Username: [____________]   │
│  Email: [____________]      │
│  Password: [____________]   │
│                             │
│  [     Register    ]        │
│                             │
│  Already have account?      │
│  [      Login      ]        │
└─────────────────────────────┘
```

- Auto-login after successful registration
- Show terms/privacy (if applicable)

### 8.5 ProfileScreen

**Layout (Bottom Sheet):**
```
┌─────────────────────────────┐
│  ━━━━━━ Handle ━━━━━━       │
│                             │
│  [Avatar]                   │
│  @username                  │
│  email@example.com          │
│                             │
│  [ Change Password ]        │
│  [ Account Settings ]       │
│  [ App Settings ]           │
│                             │
│  [      Logout     ]        │
└─────────────────────────────┘
```

- **Change Password:** Current + New password form
- **Logout:** Clear tokens, clear pending queue (or ask user), navigate to Home

### 8.6 SyncScreen

**Layout:**
```
┌─────────────────────────────┐
│  ← Sync Status              │
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ 🔄 Syncing...           │ │
│ │ Last sync: 2m ago       │ │
│ │ Network: Online ✅      │ │
│ │ Pending: 3 operations   │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ In Progress                 │
│ ┌─────────────────────────┐ │
│ │ Uploading "Cats"        │ │
│ │ Sticker 2/4 ▓▓▓▓░░░░░  │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ Pending (2)                 │
│ ┌─────────────────────────┐ │
│ │ ⏳ Update "Dogs"        │ │
│ │    Waiting...           │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ ❌ Delete sticker #3    │ │
│ │    Failed: Timeout      │ │
│ │ [Retry] [Remove]        │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ [  Sync Now  ] [Clear Done] │
└─────────────────────────────┘
```

**Real-time Updates:**
- Observe `SyncManager.operationsFlow` for live UI updates
- Active operation shown at top with progress
- Pull-to-refresh triggers manual sync
- Swipe to dismiss completed operations

---

## 9. Database Schema Changes

### 9.1 Migration Plan (v4 → v5)

**New Columns in `sticker_pack` table:**
```sql
ALTER TABLE sticker_pack ADD COLUMN cloud_id TEXT;
ALTER TABLE sticker_pack ADD COLUMN sync_state TEXT DEFAULT 'LOCAL_ONLY';
ALTER TABLE sticker_pack ADD COLUMN last_sync_at INTEGER;
ALTER TABLE sticker_pack ADD COLUMN visibility TEXT DEFAULT 'PRIVATE';
ALTER TABLE sticker_pack ADD COLUMN cloud_owner_id TEXT;
```

**New Columns in `sticker` table:**
```sql
ALTER TABLE sticker ADD COLUMN cloud_id TEXT;
ALTER TABLE sticker ADD COLUMN sync_state TEXT DEFAULT 'LOCAL_ONLY';
ALTER TABLE sticker ADD COLUMN last_sync_at INTEGER;
ALTER TABLE sticker ADD COLUMN cloud_url TEXT;
```

**New Table: `sync_queue`**
```sql
CREATE TABLE sync_queue (
    id TEXT PRIMARY KEY NOT NULL,
    type TEXT NOT NULL,
    target_id TEXT NOT NULL,
    payload TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    completed_at INTEGER,
    priority INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_sync_queue_status ON sync_queue(status);
CREATE INDEX idx_sync_queue_target ON sync_queue(target_id);
```

**New Table: `sync_metadata`**
```sql
CREATE TABLE sync_metadata (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    last_sync_at INTEGER,
    last_sync_status TEXT,
    pending_count INTEGER DEFAULT 0
);
```

### 9.2 Enum Mapping

**Visibility:**
- Local DB: `PUBLIC`, `PRIVATE`, `UNLISTED` (same as API)
- Domain Model: `PackVisibility` enum

**SyncState:**
- `LOCAL_ONLY` - Never synced
- `SYNCED` - Matches cloud
- `PENDING` - Has pending changes
- `FAILED` - Last sync failed

---

## 10. Error Handling

### 10.1 Offline Scenarios

**User Creates Pack Offline:**
1. Pack saved to local DB with `syncState = LOCAL_ONLY`
2. `SyncManager.enqueue()` called but detects offline
3. Operation saved to `sync_queue` with `status = PENDING`
4. UI shows: Snackbar "Pack saved. Will sync when online."
5. When network restored → Worker triggers → Pack uploaded

**User Edits Pack Offline:**
1. Edit saved to local DB
2. `syncState` updated to `PENDING`
3. Operation enqueued
4. UI shows: Snackbar "Changes saved. Will sync when online."
5. When online → Changes pushed to cloud

### 10.2 Auth Errors

**401 Unauthorized:**
- Ktor plugin attempts token refresh
- If refresh succeeds → retry original request
- If refresh fails → emit `AuthState.Unauthenticated`
- Navigate to LoginScreen
- Pending operations remain in queue (will retry after re-auth)

**403 Forbidden:**
- Show Snackbar: "Permission denied"
- Operation marked as FAILED (not retryable)

### 10.3 Network Errors

**Retryable (enqueue for retry):**
- Timeout, 5xx, No connectivity
- Retry with exponential backoff

**Permanent (mark as FAILED):**
- 400 Bad Request, 404 Not Found, 409 Conflict
- User can retry from SyncScreen after fixing issue

### 10.4 Conflict Resolution

**Scenario: User edits pack on Device A and Device B while offline:**
1. Both devices save to local DB and enqueue changes
2. Device A comes online first, pushes changes to cloud
3. Device B comes online, tries to push changes
4. Server returns 409 Conflict or newer `updatedAt`
5. SyncManager fetches latest server version
6. Server version wins → Local DB overwritten
7. UI updates via Flow observation
8. User sees Snackbar: "Pack updated from server"

---

## 11. Security Considerations

### 11.1 Token Storage
- Access token and refresh token encrypted at rest
- In-memory cache for active session (cleared on app kill)
- No token logging in debug builds (redact from logs)

### 11.2 API Key Management
- API base URL configurable per flavor (debug/release)
- No hardcoded credentials in source code

### 11.3 Data Privacy
- User password never stored locally (only tokens)
- Local images remain on device unless explicitly uploaded
- Cloud sync only for authenticated users

---

## 12. Testing Strategy

### 12.1 Unit Tests
- `SyncManager`: Queue processing, retry logic, conflict resolution
- `AuthManager`: Token refresh, expiry handling
- `StickerRepository`: Local + cloud integration, offline behavior

### 12.2 Integration Tests
- Full sync flow: Create pack → offline → online → verify cloud
- Auth flow: Login → token refresh → logout → verify cleanup
- Conflict resolution: Simultaneous offline edits on two devices

### 12.3 Manual Test Cases
1. Create pack offline → verify queue → go online → verify sync
2. Edit pack while syncing → verify no data loss
3. Logout while sync pending → verify queue persists
4. Re-login → verify sync resumes
5. Kill app during sync → verify worker resumes on restart

---

## 13. Implementation Order

**Phase 1: Foundation**
1. Database migration (v4 → v5): Add sync columns and sync_queue table
2. Add kotlinx-datetime dependency for timestamp handling
3. Create AuthManager with token storage (DataStore + encryption)
4. Create NetworkMonitor (expect/actual for connectivity)
5. Add AuthApiService with login/register/refresh endpoints

**Phase 2: Cloud Repository**
1. Create CloudStickerRepository with all API endpoints
2. Add Bearer token interceptor to Ktor client
3. Auto-refresh plugin for 401 responses
4. Map API DTOs to domain models

**Phase 3: Sync Engine**
1. Create SyncManager with queue management
2. Implement operation execution logic
3. Add conflict resolution (server wins)
4. Create SyncWorker (Android WorkManager + iOS BackgroundTasks)

**Phase 4: Enhanced Repository**
1. Update StickerRepositoryImpl to use SyncManager
2. Modify save/delete/update methods to enqueue cloud ops
3. Ensure all operations work offline
4. Add sync metadata tracking

**Phase 5: UI**
1. LoginScreen + RegisterScreen
2. ProfileScreen (bottom sheet)
3. HomeScreen bottom bar with profile/sync/add buttons
4. Pack card sync indicators
5. PackDetailScreen sync status icon
6. SyncScreen with queue visualization
7. Pull-to-refresh on HomeScreen

**Phase 6: Polish**
1. Error handling and Snackbar messages
2. Loading states and skeletons
3. Analytics/logging for sync operations
4. Performance optimization (batch operations)
5. Final testing and bug fixes

---

## 14. Dependencies to Add

**Common (libs.versions.toml):**
```toml
kotlinx-datetime = "0.6.1"
datastore = "1.1.1"
security-crypto = "1.1.0-alpha06"
```

**KMP commonMain:**
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime")
implementation("androidx.datastore:datastore-preferences:$datastore")
```

**Android:**
```kotlin
implementation("androidx.security:security-crypto:$security_crypto")
implementation("androidx.work:work-runtime-ktx:2.9.1")
```

**iOS:**
- No additional dependencies (use platform Keychain APIs)

---

## 15. Open Questions (None)

All requirements have been addressed:
- ✅ Offline-first with auto-sync
- ✅ No manual upload buttons
- ✅ Unified repository (Approach C)
- ✅ Timestamp standardization (UTC epoch millis)
- ✅ Background sync workers
- ✅ PackBottomBar for HomeScreen
- ✅ SyncScreen with queue visualization
- ✅ Last-write-wins conflict resolution

---

## 16. Appendix: API Endpoint Mapping

| App Feature | API Endpoint | Sync Type |
|------------|--------------|-----------|
| Create Pack | `POST /api/v1/sticker-packs` | Push |
| Update Pack | `PUT /api/v1/sticker-packs/{id}` | Push |
| Delete Pack | `DELETE /api/v1/sticker-packs/{id}` | Push |
| Add Sticker | `POST /api/v1/sticker-packs/{id}/stickers` | Push |
| Remove Sticker | `DELETE /api/v1/sticker-packs/{id}/stickers/{stickerId}` | Push |
| Reorder Stickers | `PUT /api/v1/sticker-packs/{id}/reorder` | Push |
| Upload Images | `POST /api/v1/upload` | Push |
| Get My Packs | `GET /api/v1/sticker-packs` | Pull |
| Delta Sync | `GET /api/v1/sync?lastSyncAt=` | Pull |
| Register | `POST /api/v1/auth/register` | - |
| Login | `POST /api/v1/auth/login` | - |
| Refresh Token | `POST /api/v1/auth/refresh` | - |
| Get Profile | `GET /api/v1/auth/me` | - |

---

*Design approved for implementation.*
