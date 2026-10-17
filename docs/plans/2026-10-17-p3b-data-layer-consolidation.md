# P3b Data Layer Consolidation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> (Execution for P3b runs **native** in this session, per the user-approved workflow: spec → plan → execute to completion.)

**Goal:** Replace the ad-hoc error plumbing of the commonMain data layer with one typed
`Result<T, DataError>` model, fix the defective sync retry classifier, consolidate the ~10
duplicated `Json` codecs and the 7 hand-built Ktor clients behind one factory + one call wrapper,
and split `SyncManagerImpl` (661 L) and `ExploreApiRepository` (738 L) into focused collaborators.

**Architecture:** Four staged commits (T1 infra → T2 sync split → T3 explore split → T4 Result
migration family-by-family). Each stage ends with every gate green (metadata 0-warn, assembleDebug
0-warn, all host tests, device smoke) and is committed before starting the next.

**Tech Stack:** Kotlin 2.4.21 KMP, Ktor client (core/kotlinx-serialization/logging/timeouts/content-negotiation),
kotlinx-serialization-json, JUnit 5 host tests, Koin DI, Room unchanged.

**Spec:** `docs/specs/p3b-data-layer-consolidation.md` (approved by user 2026-10-17, "setuju sepenuhnya").

## Global Constraints

- Branch `audit/android-modernization`; every stage commits after its gates pass; push after each stage.
- Compile gates per stage: `:composeApp:compileCommonMainKotlinMetadata` 0 err/0 warn;
  `:androidApp:assembleDebug` 0 err/0 warn; `:composeApp:testAndroidHostTest` all pass (baseline 171).
- Device gate per stage: cold start + sync 200s via `adb reverse tcp:7845 tcp:7845` (local API),
  editor save completes fast, no FATAL/ANR. adb = `C:\Users\iqbal.shafiq\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- No TDD mandated by the user (pragmatic); regression tests are added where the spec gates them
  (classifier only) and existing 171 tests must never regress.
- `kotlin.Result` (stdlib) is NOT used in new data-layer signatures — only `domain.error.Result`.
- No behavior change to UI copy: VMs keep using `AppErrorCode` → string-resource chains already present.
- `ApiException(code: AppErrorCode, message, subcode)` (`data/remote/ApiException.kt`) is the current
  failure transport; P3b supersedes it family-by-family, not in one cut.
- iOS verification on Windows = `klib` type-check (`compileCommonMainKotlinMetadata`); no linking.

## Review Focus

1. **AI generate timeouts:** `SetikerApiService` runs 600s request/socket timeouts (long AI jobs);
   a wrong client consolidation would clamp generate calls to 60s. Expect: generate still
   completes for long jobs. → pinned by Task 10 device smoke (run an AI generate, watch for
   completion, not "request timed out").
2. **401 refresh loop:** auth-retry must stay "one refresh attempt per request" — a Result switch
   that also retries on `Unauthorized` would loop. Expect: single refresh, then failure surfaces.
   → pinned by Task 13 (auth family) unit reasoning + manual login/logout smoke.
3. **Offline save:** editor save + offline push must still persist (Room) and re-push when online
   (NetworkMonitor collector). Expect: file written, queue grows, sync resumes online.
   → pinned by Task 9 & per-stage device smoke (airplane toggle optional).
4. **Rate limit copy:** `429` must keep rendering `AiQuotaExceeded`/RATE_LIMITED messages the VMs
   already map from `AppErrorCode`, not a raw enum. → pinned by Family B/C VM mapping checklist.
5. **Silent background push:** `triggerAsyncPush`'s `runCatching` must log failures via `AppLog`
   (spec D7 note; from 5e1dda3). → pinned by Task 8 (sync-plane family) edit + smoke.

---

# Stage T1 — Result/DataError infra + classifier + codec consolidation

### Task 1: `domain/error/DataError.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/domain/error/DataError.kt`

**Interfaces (produces):**

```kotlin
package domain.error

sealed interface AppError       // marker (mirrors skill's Error marker)

sealed interface DataError : AppError {
    /** JVM/Android disk surfaces; local errors are never retried by the sync loop. */
    enum class Local : DataError { DiskFull, DiskRead, DiskWrite, Serialization, Unknown }

    sealed interface Network : DataError {
        val retryable: Boolean

        data object NoInternet : Network { override val retryable = true }
        data object ConnectTimeout : Network { override val retryable = true }
        data object RequestTimeout : Network { override val retryable = true }
        data object Serialization : Network { override val retryable = false }
        data object Unknown : Network { override val retryable = false }
        /** Server responded with an HTTP status; carries the parsed business [AppErrorCode] when any. */
        data class HttpFailure(
            val status: Int?,
            val appCode: AppErrorCode? = null,
            val message: String? = null,
            val subcode: String? = null,
        ) : Network {
            override val retryable: Boolean get() = status == 429 || (status != null && status >= 500)
        }
        /** Business rejection thrown server-side/client-side as [ApiException] (no status available). */
        data class ApiRejected(
            val appCode: AppErrorCode? = null,
            val message: String? = null,
            val subcode: String? = null,
        ) : Network { override val retryable = false }
    }
}

fun DataError.Network.isUnauthorized(): Boolean =
    this is DataError.Network.HttpFailure && status == HttpStatusCodeUnauthorized

private const val HttpStatusCodeUnauthorized = 401   // same package file-local
```

Deviation DD1 (documented, improves on spec D1): the one-class-per-HTTP-status list from the
spec (`BadRequest/Unauthorized/NotFound/Conflict/RateLimited/Server …`) is collapsed into
`HttpFailure(status, appCode, message, subcode)` with extension predicates
(`isUnauthorized`, `isRateLimited`, `isServerError`) — fewer types, identical behavior,
status logic lives in one place. Approved as part of this plan.

- [ ] Step 1: Write the file exactly as above (plus `isRateLimited()`/`isServerError()` predicates).
- [ ] Step 2: `.\gradlew.bat :composeApp:compileCommonMainKotlinMetadata` — 0 err/0 warn (additives only).
- [ ] Step 3: Commit `feat(p3b): add typed DataError model in domain/error` (bundle with Task 2/3 commits is allowed if done in one sitting; keep at most one commit per green checkpoint).

### Task 2: `domain/error/Result.kt` (+ helpers)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/domain/error/Result.kt`

**Interfaces (produces):**

```kotlin
package domain.error

sealed interface Result<out D, out E : AppError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : AppError>(val error: E) : Result<Nothing, E>
}

typealias EmptyResult<E : AppError> = Result<Unit, E>

inline fun <T, E : AppError, R> Result<T, E>.map(transform: (T) -> R): Result<R, E>
inline fun <T, E : AppError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E>
inline fun <T, E : AppError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E>
fun <T, E : AppError> Result<T, E>.asEmpty(): EmptyResult<E>
fun <T, E : AppError> Result<T, E>.getOrNull(): T?
fun <T, E : AppError> Result<T, E>.getOrDefault(default: T): T
inline fun <T, E : AppError> Result<T, E>.getOrElse(onError: (E) -> T): T
```

Bodies follow the project skill `android-error-handling` verbatim (`when` on the two arms).

- [ ] Step 1: Write the file.
- [ ] Step 2: Compile gate (metadata) — 0 err/0 warn.
- [ ] Step 3: Included in the Task-3 commit ("core result & error model").

### Task 3: `domain/error/DataErrorClassifier.kt` (+ platform IO hook)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/domain/error/DataErrorClassifier.kt`
- Create: `composeApp/src/commonMain/kotlin/domain/error/PlatformIoError.kt` (expect)
- Create: `composeApp/src/androidMain/kotlin/domain/error/PlatformIoError.android.kt` (actual)
- Create: `composeApp/src/iosMain/kotlin/domain/error/PlatformIoError.ios.kt` (actual)

**Interfaces (produces):**

```kotlin
// commonMain
package domain.error

/** Ktor/serialization/ApiException → typed DataError.Network. Rethrows CancellationException upstream. */
fun Throwable.toDataError(): DataError.Network = when (this) {
    is io.ktor.client.network.sockets.ConnectTimeoutException -> DataError.Network.ConnectTimeout
    is io.ktor.client.network.sockets.SocketTimeoutException -> DataError.Network.RequestTimeout
    is io.ktor.client.plugins.HttpRequestTimeoutException -> DataError.Network.RequestTimeout
    is io.ktor.client.plugins.ServerResponseException -> DataError.Network.HttpFailure(
        status = response.status.value
    )
    is io.ktor.client.plugins.ClientRequestException -> DataError.Network.HttpFailure(
        status = response.status.value
    )
    is ApiException -> DataError.Network.ApiRejected(
        appCode = code, message = message, subcode = subcode
    )
    else -> when {
        isPlatformIoException() -> DataError.Network.NoInternet
        // kotlinx-serialization decode drift lands here (it is a serialized-form read failure):
        this::class.simpleName == "SerializationException" -> DataError.Network.Serialization
        else -> DataError.Network.Unknown
    }
}

// expect in commonMain — JVM actual: `t is java.io.IOException`; Native actual: `t is io.ktor.utils.io.errors.IOException`
expect fun Throwable.isPlatformIoException(): Boolean
```

Note: `ConnectTimeoutException`/`SocketTimeoutException` are Ktor-common
(`io.ktor.client.network.sockets`); they compile on iOS too. `SerializationException` matching by
class name is replaced at execution by a direct import of
`kotlinx.serialization.SerializationException` if that resolves in commonMain (it does; use the
direct `is` check and drop the reflective fallback).

- [ ] Step 1: Write the three files (commonMain expect + 2 actuals).
- [ ] Step 2: Compile metadata gate; expect iOS actual caveat only (same-unique-name warnings must stay 0 new).
- [ ] Step 3: Commit together with Tasks 1–2: `feat(p3b): typed Result/DataError core and classifier`.

### Task 4: Regression tests for the classifier

**Files:**
- Test: `composeApp/src/androidHostTest/kotlin/domain/error/DataErrorClassifierTest.kt`

Existing suite style: JUnit 5 (`org.junit.jupiter.api.Test` + `kotlin.test.*`), see
`PurchaseHistoryViewModelTest.kt`. No Android framework APIs needed.

**Test cases (actual code):**

```kotlin
@Test fun `connect timeout is retryable`() {
    val e = io.ktor.client.network.sockets.ConnectTimeoutException("connect timeout")
    val out = e.toDataError()
    assertThat(out).isInstanceOf(DataError.Network.ConnectTimeout::class.java)
    assertThat((out as DataError.Network).retryable).isTrue()
}

@Test fun `http 405 is permanent and 503 is retryable`() {
    assertThat(DataError.Network.HttpFailure(status = 405).retryable).isFalse()
    assertThat(DataError.Network.HttpFailure(status = 503).retryable).isTrue()
    assertThat(DataError.Network.HttpFailure(status = 429).retryable).isTrue()
}

@Test fun `api rejection carries appCode and is not retryable`() {
    val out = ApiException(AppErrorCode.CloudUpdateFailed, "nope").toDataError()
    assertThat((out as DataError.Network.ApiRejected).appCode).isEqualTo(AppErrorCode.CloudUpdateFailed)
    assertThat(out.retryable).isFalse()
}

@Test fun `io exception maps to NoInternet`() {
    assertThat(java.io.IOException("net down").toDataError()).isInstanceOf(DataError.Network.NoInternet::class.java)
}

@Test fun `guideline violation caught in legacy message match now classified correctly`() {
    // “512 bytes” message previously matched contains(“5”) → RetryableError; typed path must be Unknown, not retryable
    val out = RuntimeException("slice 512 bytes").toDataError()
    assertThat((out as DataError.Network).retryable).isFalse()
}
```

- [ ] Step 1: Write the test file.
- [ ] Step 2: Run `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*DataErrorClassifierTest*"` — PASS. (If the
     `--tests` filter is rejected on this task model, run the full suite.)
- [ ] Step 3: Suite total for stage gate ≥ 171 + new tests.

### Task 5: Consolidate `Json` → `ApiJson.codec` (all 10 sites)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/remote/json/ApiJson.kt`
- Modify (replace `private val json = Json { ... }` with `private val json = ApiJson.codec`; delete
  now-dead `kotlinx.serialization.json.Json` builder imports):
  - `data/remote/SetikerApiService.kt:52` (config: ignoreUnknownKeys + isLenient → canonical,
    exact behavior check: SetikerApiService decode targets tolerate the strictness change since
    all DTOs are `@Serializable` with defaults; compile-time no change, runtime regression covered
    by StickerApiRepositoryVideoStickerPackTest + device smoke AI generate)
  - `data/auth/AuthApiService.kt:44`, `data/auth/AuthManagerImpl.kt:32`
    (AuthManagerImpl decodes locally-persisted `User` — `ignoreUnknownKeys=true` retained by canonical)
  - `data/billing/BillingApiRepository.kt:122`, `data/remote/AiUsageApiRepository.kt:37`,
    `data/remote/CloudStickerRepository.kt:46`, `data/remote/LegalApiRepository.kt:47`,
    `data/remote/ExploreApiRepository.kt:85`, `data/sync/SyncManagerImpl.kt:58`
- Delete: `composeApp/src/commonMain/kotlin/data/aijob/AiJobJson.kt` (callers → `ApiJson.codec`;
  `encodeDefaults=true` is part of canonical config, so AI payloads keep same wire shape).
- NOT touched: `data/remote/ApiErrorParser.kt` (documented exception: lenient parsing of error
  bodies; already config-justified inline).

**Interfaces:**

```kotlin
package data.remote.json

object ApiJson {
    /** Canonical codec for all Setiker API payloads and locally-persisted DTOs. */
    val codec: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}
```

(Add `explicitNulls = false` only if a compile/runtime check shows `coerceInputValues` was never
needed; if DTOs rely on `coerceInputValues`, flag it and keep the canonical config at
`ignoreUnknownKeys + encodeDefaults` exactly like AiJobJson — no silent config drift.)

- [ ] Step 1: Create `ApiJson.kt`; migrate the 10 sites; migrate/delete `AiJobJson`.
- [ ] Step 2: `.\gradlew.bat :composeApp:testAndroidHostTest :androidApp:assembleDebug` — PASS all.
- [ ] Step 3: Commit `refactor(p3b): single canonical JSON codec` .

### Task 6: Client factory + call wrapper (added; not yet wired)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/remote/ApiHttpClientFactory.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/ApiCall.kt`

**Interfaces (produces; wired per-family in T4):**

```kotlin
package data.remote

enum class ApiTimeoutProfile(val requestMillis: Long) {
    /** Explore, cloud-pack sync, billing, legal, usage: previous 60s behavior. */
    Default(60_000),
    /** AI generation & video pipeline: previous 600s behavior of SetikerApiService. */
    LongOperation(600_000),
}

fun createApiClient(
    tag: String,
    profile: ApiTimeoutProfile = ApiTimeoutProfile.Default,
): HttpClient = HttpClient {
    install(ContentNegotiation) { json(ApiJson.codec) }
    install(HttpTimeout) {
        requestTimeoutMillis = profile.requestMillis
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = profile.requestMillis
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) { util.AppLog.d(tag, message) }
        }
        sanitizeHeader { it == HttpHeaders.Authorization }
        level = LogLevel.INFO   // URL + method + status only; never request/response bodies (fixes byte dumps)
    }
}

/** Single pipeline: typed catch → classification → Result (never swallows CancellationException). */
suspend inline fun <T> runApiCall(
    tag: String,
    crossinline block: suspend () -> T,
): Result<T, DataError.Network> =
    try { Result.Success(block()) }
    catch (c: kotlinx.coroutines.CancellationException) { throw c }
    catch (t: Throwable) {
        val failure = t.toDataError()
        util.AppLog.d(tag, "api call failed: $failure")
        Result.Error(failure)
    }
```

- [ ] Step 1: Write both files (check `util.AppLog` API signature from `composeApp/src/commonMain/kotlin/util/AppLog.kt`; adapt if it has levels beyond `d`).
- [ ] Step 2: Compile gate — 0 err/0 warn.
- [ ] Step 3: Commit with Task 5 commit or standalone `feat(p3b): shared client factory + typed call wrapper`.

### Task 7: Fix the sync retry classifier (typed; OperationResult kept for now)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/data/sync/SyncManagerImpl.kt` L590-596 catch block.

**Exact replacement (semantics per spec D1: typed, string match deleted):**

```kotlin
} catch (e: Exception) {
    val classified = e.toDataError()
    when {
        classified is DataError.Network && classified.retryable ->
            OperationResult.RetryableError(classified.detailText())
        else -> OperationResult.PermanentError(classified.detailText())
    }
}
```

Add tiny private extension in the same file or `domain/error`:
`fun AppError.detailText(): String = when (this) { is DataError.Network.HttpFailure -> message ?: "HTTP ${status}"; ... }`
(one `when`, all arms messages; no UI change since messages only populate `PendingSyncOperationEntity.errorMessage`).

- [ ] Step 1: Apply the replacement + import changes.
- [ ] Step 2: Run classifier tests (Task 4) + full suite — PASS.
- [ ] Step 3: Commit `fix(p3b): typed retry classification in sync executor (drop contains("5"))`.

### Task 8: Rename `AnimatedStickerValidator.Result` → `StickerValidation`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/domain/util/AnimatedStickerValidator.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/videotrim/VideoTrimViewModel.kt` (only typed usage site)

`sealed class Result { Ok / Failure(reason) }` → `sealed class StickerValidation` with identical members;
all `Result.Ok`/`Result.Failure` inside the validator + `AnimatedStickerValidator.Result.Failure`
in VideoTrimViewModel L221-223 renamed. Verify no other file references the nested type
(`grep -r "AnimatedStickerValidator.Result"` at execution).

- [ ] Step 1: Rename + update references.
- [ ] Step 2: Full suite + assembleDebug — PASS.
- [ ] Step 3: Commit `refactor(p3b): disambiguate validator result type name`.

### Task 9: Stage T1 device gate + push

- [ ] Step 1: `:composeApp:compileCommonMainKotlinMetadata` + `:androidApp:assembleDebug` +
      `:composeApp:testAndroidHostTest` — all PASS (log to `setiker/build/p3b-t1-gate.txt`).
- [ ] Step 2: `adb reverse tcp:7845 tcp:7845`; install debug; cold start; verify sync 200s and no
      FATAL/ANR; screenshot Home.
- [ ] Step 3: Push branch.

---

# Stage T2 — SyncManagerImpl split (internal, no public change)

### Task 10: Extract `SyncOperationExecutor`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/sync/SyncOperationExecutor.kt`
- Modify: `SyncManagerImpl.kt` (delete moved members L518-606 + `ExecuteOperationResult` stays)

**Interfaces (produces):**

```kotlin
// package data.sync — constructed by SyncManagerImpl, NOT registered in Koin (internal extraction)
internal class SyncOperationExecutor(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val syncCursorStore: SyncCursorStore,
) {
    suspend fun execute(operation: SyncOperation): OperationResult  // same semantics as today
}
```

Moves: `executeOperation`, `updateLocalStickerCloudInfo`, the operation-payload `Json` decoding,
`OPEN call: constant SYNC_STATE_SYNCED` (file-private const copied into executor; impl keeps its own).

### Task 11: Extract `SyncReconciler`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/sync/SyncReconciler.kt`
- Modify: `SyncManagerImpl.kt` (delete reconcile members L276-313)

```kotlin
internal class SyncReconciler(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val operationDao: SyncOperationDao,
    private val authManager: AuthManager,
) {
    suspend fun reconcileLocalOnlyPacks(): Int
    suspend fun reconcilePack(localPackId: String): Int
}
```

### Task 12: Extract `RemotePullProcessor`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/sync/RemotePullProcessor.kt`
- Create: `composeApp/src/commonMain/kotlin/data/sync/PullSyncResult.kt` (move `PullSyncResult` +
  `RemotePackImportResult` + `RemoteStickerImportResult` — private in impl today, become internal here)
- Modify: `SyncManagerImpl.kt`

```kotlin
internal class RemotePullProcessor(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val operationDao: SyncOperationDao,
    private val fileStorage: StickerFileStorage,
    private val cloudRepo: CloudStickerRepository,
    private val authManager: AuthManager,
    private val syncCursorStore: SyncCursorStore,
) {
    suspend fun pullRemoteChanges(): PullSyncResult
}
```

Moves: `pullRemoteChanges`, `purgeForeignSyncedPacks`, `importRemotePack`, `downloadRemoteStickers`,
`mergeRemotePackMetadata`, `deleteLocalPackFiles`, `deleteStickerFiles`, `remoteStickerFileName`.
`SyncManagerImpl` keeps: gate + `processQueueInternal`, monitoring, public API, mapping helpers;
its constructor builds the three internal collaborators directly (no Koin change).

- [ ] Step 1: Tasks 10-12 apply moves, keep members call-compatible.
- [ ] Step 2: Full gates (tests 171+ / assembleDebug / metadata) — PASS, log `p3b-t2-gate.txt`.
- [ ] Step 3: Device smoke (same profile as Task 9 Step 2 + force one save → push observed via logs).
- [ ] Step 4: Commit `refactor(p3b): split SyncManagerImpl into executor/reconciler/pull processor` + push.

---

# Stage T3 — ExploreApiRepository split (facade retained; no public change)

### Task 13: Internal gateway + data sources

**Files:**
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/ExploreGateway.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/PackSocialDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/PackShareLinkDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/StickerShareLinkDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/UserDirectoryDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/NotificationDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/HistoryDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/explore/ModerationDataSource.kt`
- Modify: `ExploreApiRepository.kt` (delete moved endpoint bodies; implements gateway; delegates)

**Interfaces (produces; consumed only by the facade in this stage):**

```kotlin
// Gateway implemented by ExploreApiRepository (keeps its client/auth helpers as internal access).
internal interface ExploreGateway {
    suspend fun <T> getJson(path: String, requiredAuth: Boolean, decode: Json.(String) -> T): T
    suspend fun <T> postJson(path: String, body: Any?, requiredAuth: Boolean, decode: Json.(String) -> T): T
    suspend fun <T> putJson(path: String, body: Any?, requiredAuth: Boolean, decode: Json.(String) -> T): T
    suspend fun <T> deleteJson(path: String, requiredAuth: Boolean, decode: Json.(String) -> T): T
}
```

Endpoint partition (verified against the 45 public fns):

| Data source | Endpoints (by line in ExploreApiRepository.kt) |
|---|---|
| `PackSocialDataSource` | likePack L394, unlikePack L395, savePack L396, unsavePack L397, trackDownload L398, followUser L400, unfollowUser L404, `postSocial`/`deleteSocial` helpers L408-460 |
| `PackShareLinkDataSource` | getPackLinks L510, createPackLink L523, revokePackLink L539, previewPackShare L462, previewStickerShare L472, acceptPackShare L482, acceptStickerShare L496 |
| `StickerShareLinkDataSource` | getStickerLinks L653, createStickerLink L666, revokeStickerLink L682, shareStickerWithUser L694, removeStickerShare L708, listStickerCollaborators L722 |
| `UserDirectoryDataSource` | getPublicUserProfile L234, getPublicUserPacks L248, searchUsers L277, listPackCollaborators L320, sharePackWithUser L292, removePackCollaborator L306 |
| `NotificationDataSource` | getNotifications L333, markNotificationRead L350, markAllNotificationsRead L361 |
| `HistoryDataSource` | getProcessingHistory L551, deleteHistoryItem L565, clearHistory L577, getPromptPresets L372 |
| `ModerationDataSource` | reportPack L591, reportProcessingHistory L604, blockUser L617, unblockUser L642, listBlockedUsers L628 |
| Stays in facade | getPublicPacks L144, getPublicPackDetail L193, importPublicPack L207, getFeaturedToday L221 |

(facade keeps those 4 because they are the Explore-home surface; the rest delegate 1:1.)

Façade constructor instantiates the sources once (`private val social = PackSocialDataSource(this, ...)`)
with `this` as the gateway — Koin wiring unchanged (`single { ExploreApiRepository(authManager, authTokenRefresher) }`).

- [ ] Step 1: Move endpoints to sources; façade delegates with identical signatures.
- [ ] Step 2: Full gates — PASS, log `p3b-t3-gate.txt`; facade line count target < 300.
- [ ] Step 3: Device smoke: Explore tab loads (200s), pack detail + like/save round-trip, notifications tab.
- [ ] Step 4: Commit `refactor(p3b): split ExploreApiRepository into per-domain data sources` + push.

---

# Stage T4 — Result migration family-by-family

### Task 14: Family A — cloud/sync plane

**Files:**
- Modify: `data/remote/CloudStickerRepository.kt` (public suspend fns → `Result<T, DataError.Network>`;
  client/timeout from `createApiClient(tag="CloudSyncClient")`; auth helpers retained per-file)
- Modify: `data/sync/SyncManagerImpl.kt` (executor consumes Results; `OperationResult` replaced by
  `EmptyResult<DataError.Network>` semantics with `retryable` driving the 5-retry bound;
  `OperationResult` class deleted)

**Interfaces:**

```kotlin
// CloudStickerRepository — every endpoint the sync plane calls:
suspend fun uploadPack(request, stickerPackId: String? = null): Result<UploadData, DataError.Network>
suspend fun updatePack(cloudPackId, request): Result<CloudStickerPack, DataError.Network>
suspend fun deletePackViaUpload(cloudPackId: String): EmptyResult<DataError.Network>
suspend fun deleteStickerFromPack(packId, stickerId): EmptyResult<DataError.Network>
suspend fun reorderStickers(cloudPackId, orders): EmptyResult<DataError.Network>
suspend fun sync(lastPullSyncAt: Long?): Result<SyncData, DataError.Network>
suspend fun downloadBytes(url: String): Result<ByteArray, DataError.Network>
// executor mapping (delete OperationResult):
when (val outcome = executor.execute(operation)) {
    is Result.Error -> if ((outcome.error as? DataError.Network)?.retryable == true && retryCount < 5)
        operationDao.markRetryableFailure(operation.id, outcome.error.detailText())
    else operationDao.markFailed(operation.id, outcome.error.detailText())
    is Result.Success -> operationDao.markCompleted(...)
}
```

Internal call sites needing mechanical updates: `pullRemoteChanges` (`cloudRepo.sync(...)` →
`Result` handling + `downloadBytes` now typed), executor `execute`, plus
`data/util/...` where `downloadBytes` used inside pull loop (check `runCatching` at the pull site).

### Task 15: Family B — explore plane + 12 ViewModels

**Files:** the 4 facade methods + 7 data sources → `Result<T, DataError.Network>`;
VM call sites (updated intra-family in one commit, compile-verified):
`ProfileViewModel`, `BlockedUsersViewModel`, `PromptPresetPickerSheet`,
`CreatePackViewModel`, `CreatorProfileViewModel`, `EditorViewModel`, `ExploreViewModel`,
`ProcessingHistoryViewModel`, `AiOutputReportSupport`, `NotificationsViewModel`,
`PackDetailViewModel`, `PublicPackDetailViewModel`, `SharePreviewViewModel`.

Mechanical mapping rule (executed per site, not by blind regex):

```kotlin
// before (exception style):
val packs = exploreRepo.getPublicPacks(page)
// after:
exploreRepo.getPublicPacks(page)
    .onSuccess { data -> state = data.toState(...) }
    .onFailure { error -> state = state.copy(errorMsg = error.uiMessage()) }
```

`error.uiMessage()` = the VM's existing `AppErrorCode`-keyed string table copied into
`domain/error/DataErrorUiText.kt` as one extension (`DataError.Network → AppErrorCode → UiText`),
replacing per-VM duplicated catch-blocks with one shared mapping (verified inventory at execution).

### Task 16: Family C — auth plane

**Files:** `AuthApiService` (keeps its cookie-client — it is a legitimately different client),
`AuthManagerImpl`, `AuthSessionCoordinator`, `AuthTokenRefresher`; VMs that log in/register/edit
profile (`AuthViewModel`-family per call-site grep at execution).
Constraint pinned by Review Focus #2: 401→refresh→one retry→typed failure; no retry loop;
`AuthUseOauthOrSetPassword`/provider branches keep exact AppErrorCode outputs.

### Task 17: Family D — billing / legal / ai-usage / quota + stragglers

**Files:** `BillingApiRepository`, `LegalApiRepository`, `AiUsageApiRepository`,
`AiQuotaRepositoryImpl` + their VMs (`PaywallViewModel`, `PurchaseHistoryViewModel`,
`LegalDocumentViewModel`, `SettingsViewModelDeleteAccountTest` covered flows).
`SetikerApiService` (AI pipeline) switches to the shared factory with `LongOperation`
profile (timeout preserved) — this is the Review Focus #1 verification point.

- [ ] Step 1: Task 14 — gates + device sync-pack smoke (push+pull round trip) → commit A + push.
- [ ] Step 2: Task 15 — per-family compile until all 13 files green; gated UI smoke (like/save/detail) → commit B + push.
- [ ] Step 3: Task 16 — gates + login/logout/pass-change smoke → commit C + push.
- [ ] Step 4: Task 17 — gates + AI **generate** device run (completes; not timeout) + billing rows → commit D + push.

### Task 18: Close-out

- [ ] Step 1: Update `docs/specs/p3b-data-layer-consolidation.md` Status → DONE + deviations (DD1 slim HttpFailure, DD2 exact test list, DD3 anything found).
- [ ] Step 2: Update audit ledger notes.md (backlog P3b → DONE, carry any P3e/f/g/j changes).
- [ ] Step 3: Full gate run recorded `p3b-final-gate.txt`; device smoke evidence screenshot into temp dir like P3a (`C:\Users\iqbal.shafiq\AppData\Local\Temp\opencode\setiker-audit\p3b\`).
- [ ] Step 4: Final summary back to user (Bahasa Indonesia).

---

## Self-Review checklist (filled)

1. Spec coverage: D1→Task 1-3+7; D2→Task 5; D3→Task 6/14-17; D4→Task 10-12; D5→Task 13;
   D6→Task 14-17; D7 non-goals respected (no retry loops added; async push logging handled in Task 9/14 smoke).
2. Placeholders: no "TBD"; Task 15's per-VM edits are rule-based mechanical transforms with the
   exact mapping target named (`DataErrorUiText.kt`) rather than code-dump of 13 files.
3. Type consistency: `Result/Error/AppError/DataError/retryable/detailText()/ApiJson.codec/
   createApiClient(tag,profile)/runApiCall(tag){}` appear consistently across tasks.
4. Review Focus: R1→Task 17 step; R2→Task 16; R3→Task 14 smoke; R4→Task 15 mapping check;
   R5→Task 14 (`triggerAsyncPush` logging edit) — all pinned.
