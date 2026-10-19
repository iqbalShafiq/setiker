# P3b — Data Layer Error Model & Infrastructure Consolidation

Status: DONE — executed 2026-10-19 on `audit/android-modernization` (infra `504fabc`, Family A
`f5798fe`, Family B `b3db044`, Family C `5d11e3a`, Family D `ee45e8a`). Final gates: 180 tests /
0 failures / 0 errors, metadata compile + `assembleDebug` green; device smoke and the executed
deviations (DD1–DD5) are recorded in `docs/plans/2026-10-17-p3b-data-layer-consolidation.md`.
Scope: commonMain data layer refactor. No server/API contract change, no UI behavior change
(except strictly-better failure visibility where messages already existed).
Target toolchain: unchanged from P3a (AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.21, Compose Multiplatform 1.12.1, Room 2.8.5).

---

## Context

The audit backlog (P3b) names four data-layer problems. Each was re-verified against HEAD
(`5e1dda3`) before writing this spec:

### F1. No shared typed error/result model — and one broken retry classifier

- Three competing hand-rolled "result" types:
  - `data.sync.SyncManagerImpl.OperationResult` (Success / RetryableError / PermanentError) —
    SyncManagerImpl.kt L640,
  - `domain.util.AnimatedStickerValidator.Result` — a `sealed class Result` shadowing the name of
    the Kotlin stdlib type (3 call-site files),
  - `domain.model.SyncResult` (used by `SyncReport`, legitimate domain reporting — stays).
- Repositories communicate failures through thrown exceptions (`runCatching` appears 17× in
  `data/**`, including `StickerApiRepository`, `StickerRepositoryImpl`, `ExploreApiRepository`,
  `AuthManagerImpl`, `BillingRepositoryImpl`, `AiJobRunner`) or by returning `null`/
  `emptyList()`/booleans, which callers cannot distinguish from "server said so".
- **Defective retry classification** in `SyncManagerImpl.executeOperation` (L590-596):

  ```kotlin
  e.message?.contains("timeout", ignoreCase = true) == true -> RetryableError
  e.message?.contains("5", ignoreCase = true) == true -> RetryableError   // any message containing "5"
  else -> PermanentError
  ```

  Every message containing the character `5` (e.g. `HTTP 405`, `slice 512 bytes`,
  `ReorderStickersSyncPayload`) is classified retryable; every timeout that phrased differently
  (`ConnectTimeoutException` carries no "timeout" in `message` on all Ktor versions) is
  classified permanent. This makes queue ops retry wrong things and permanently fail
  right things.
- There is already a server-error domain foundation: `domain.error.AppErrorCode` (49-line enum),
  `AppException`, `serverSubcodeToAppErrorCode(...)` (used by `ApiErrorParser`). P3b extends this
  foundation rather than replacing it.

### F2. `Json` is instantiated ~10 times with ad-hoc configs

Instantiation sites (grep `Json {` / `Json.default`): `AiJobJson` (canonical AI codec,
`ignoreUnknownKeys + encodeDefaults`), `AuthApiService`, `AuthManagerImpl`,
`BillingApiRepository`, `AiUsageApiRepository`, `ApiErrorParser` (`ignoreUnknownKeys +
isLenient`), `CloudStickerRepository`, `ExploreApiRepository`, `LegalApiRepository`,
`SetikerApiService`, `SyncManagerImpl` (`ignoreUnknownKeys`). Config drift between these
(decode/encode behavior, leniency) is currently unmotivated and untested.

### F3. Giant files, no source separation

- `SyncManagerImpl.kt` — 661 lines, single class owning: queue orchestration & gate,
  push-operation execution (6 operation types), reconciliation, full pull pipeline
  (pack import/merge, sticker download, foreign-pack purge, file cleanup), and mapping.
- `ExploreApiRepository.kt` — 738 lines, ~45 `suspend` endpoints spanning pack social
  (like/save/download/follow), pack/sticker share links & acceptance, user directory
  (profile/search/blocked), notifications, processing history, and moderation reports —
  called from 14 files (12 ViewModels + DI + itself).

### F4. HTTP plumbing is per-file hand-rolled

- Each remote file re-implements: error-message extraction (e.g. `ExploreApiRepository
  .extractMessage` duplicates `ApiErrorParser.extractMessage`), request/response logging
  (`Ktor LogLevel.ALL` currently dumps **multipart binary bodies** of sticker uploads to
  `System.out` via the `CloudSyncClient` logger — measured earlier in the audit), and error
  propagation.

References: the project's installed skills `android-error-handling` (typed `Result<T, E>`,
`DataError`, `map`/`onSuccess`/`onFailure`, `EmptyResult`) and `android-data-layer`
(data source vs repository boundaries, offline-first sync shape). NIA (`android/nowinandroid`)
informs the offline-first repository methodology; our implementation follows the project skills
because they match the applied error-handling contract.

---

## Goal

One typed, testable data layer:

1. A single `Result<T, E: Error>` + `DataError` model in `domain/error`, bridged to the existing
   `AppErrorCode`, with correct retry classification (typed, not string-matched).
2. One canonical `Json` codec (+ explicitly named variants only when justified).
3. One shared "make an API call" pipeline that owns: typed exception → `DataError` mapping,
   API error-envelope parsing, request/response logging that never dumps binary bodies.
4. `SyncManagerImpl` and `ExploreApiRepository` split into focused collaborators without any
   public-behavior change for the initial split stage.
5. Public repository APIs migrated to `Result<T, DataError>` family-by-family.

---

## Decisions

### D1. Types (per `android-error-handling` skill, adapted in-package)

`domain/error/` package gains:

- `sealed interface Error` (marker, per skill).
- `DataError : Error` with `sealed interface Local : DataError` (DiskFull, DiskRead, DiskWrite,
  Serialization) and `sealed interface Network : DataError` (BadRequest(408-timeout variants),
  Unauthorized, NotFound, Conflict, RateLimited, Server(5xx + status), RequestTimeout,
  ConnectTimeout, Serialization, Unknown(offType/ifaktırThrowable)).
- `sealed interface Result<out D, out E : Error>` with `Success<D>`, `Error<E>`; `typealias
  EmptyResult<E> = Result<Unit, E>`; extension helpers `map`, `onSuccess`, `onFailure`,
  `asEmpty`, `getOrNull`, `getOrDefault`, `getOrElse` — located next to the type like the skill.
- `Throwable.isRetryableDataError()` / `Throwable.toDataError(): DataError.Network` — typed
  classification on Ktor exceptions (`ConnectTimeoutException`, `SocketTimeoutException`
  primitives on both platforms via `expect/actual` only where needed), `ClientResponseException`
  status mapping (401/403 → Unauthorized, 404 → NotFound, 409 → Conflict, 429 → RateLimited,
  5xx → Server, other 4xx → typed default), `IOException` → ConnectTimeout-class retryable.
  This is the single implementation; the sync executor and any future retry path call it.
  String matching is deleted.
- Bridge to `AppErrorCode`: `DataError.Network` carries an optional `renderer: AppErrorCode?`
  (parsed by `ApiErrorParser`) so UI message lookup keys on the enum already referenced by
  ViewModels — no message strings move.

Naming conflict resolution: `domain/util/AnimatedStickerValidator.Result` is renamed to
`StickerValidation` (update at its 3 call files). Kotlin stdlib `kotlin.Result` is not used in
new data-layer signatures.

### D2. Canonical JSON codec

`data/remote/json/ApiJson.kt`: `val codec: Json` with `ignoreUnknownKeys = true`,
`encodeDefaults = true`, `isLenient = false` (strict decode; `ApiErrorParser` stays tolerant
because it parses *error* bodies of unknown shape, keeping its own config is justified and
documented). Audit all ~10 sites: they migrate to `ApiJson.codec`; `AiJobJson` is folded in
only if its `encodeDefaults = true` behavior is preserved (it is, via the canonical config);
`ApiErrorParser.json` stays as the single documented exception. Any remaining variant must be
justified in code review language ("why different") or removed.

### D3. Shared API-call pipeline

`data/remote/ApiCall.kt`:

- `suspend inline fun <reified T> HttpClient.apiGet/apiPost/apiDelete/Patch/Upload(...)` —
  or a minimal `suspend fun <T> HttpClient.apiCall(block: suspend () -> T): Result<T,
  DataError.Network>` wrapper (exact shape decided in the plan) that:
  1. runs the Ktor call,
  2. catches + classifies via D1 (`toDataError`),
  3. on non-2xx, parses the error body via `ApiErrorParser` (preserving `AppErrorCode` mapping),
  4. logs request URL + classification (`AppLog.d`) — **never request/response bodies**
     (fixes the multipart dump while keeping `LogLevel.NONE` at the engine and explicit URL
     logs instead),
  5. is the only call site responsible for wrapping responses into `Result`.
- `SetikerApiService` / `ExploreApiRepository` / `CloudStickerRepository` / `AuthApiService` /
  `LegalApiRepository` / `BillingApiRepository` / `AiUsageApiRepository` delegate collaborations
  to it during stage T4; before that they keep working as-is (stage T1 adds the helper but does
  not rip call sites yet).

### D4. Sync split — structural, no public change (stage T2)

`data/sync/` splits `SyncManagerImpl` (661 L) into:

- `SyncOperationExecutor` — `executeOperation` + payload decoding per operation type
  (CREATE/UPDATE_PACK/UPDATE_PACK_VISIBILITY/DELETE_PACK/DELETE_STICKER/REORDER).
- `SyncReconciler` — `reconcileLocalOnlyPacks`, `reconcilePack», `reconcilePackIfNeeded`, plus
  `data/sync/PackSyncDecision*.kt` helpers already in this package.
- `RemotePullProcessor` — `pullRemoteChanges`, `importRemotePack`, `downloadRemoteStickers`,
  `mergeRemotePackMetadata`, `purgeForeignSyncedPacks`, delete-file helpers, filename logic.
- `SyncManagerImpl` remains the `SyncManager` implementation and owns: `syncRunGate` +
  `processQueueInternal` orchestration (stages, gating, reporting), trait monitoring wiring,
  and the finite retry loop, now classifying via D1 (`DataError.Network` retryability +
  a bounded 5 retries) instead of string parsing.
- `SyncRunGate`, `PullSyncResult` family stay in `data/sync`. `SyncReport`/`SyncResult` stay
  in domain (reporting shape unchanged).

### D5. Explore split — structural, no public change (stage T3, after D3)

`data/remote/explore/` package (in `commonMain`) with data sources, each taking the
`HttpClient`-facing service it needs:

- `PackSocialDataSource` (like/unlike/save/unsave/download/track),
- `PackShareLinkDataSource` + `StickerShareLinkDataSource` (links CRUD, share tokens accept),
- `UserDirectoryDataSource` (profile, packs, follow/unfollow, blocked list),
- `NotificationDataSource` (list, read, readAll),
- `HistoryDataSource` (list, delete, clear),
- `ModerationDataSource` (report pack/sticker, block/unblock).
`ExploreApiRepository` stays as the injected façade delegating to these; call-site files are
untouched in this stage. Behavior-preserving is the acceptance bar.

### D6. Result migration — family-by-family (stage T4, largest)

Public `suspend` APIs of remote repositories return `Result<T, DataError.Network>` in this
order (each sub-stage: one or two repository families + their ViewModels mapped in the same
commit so nothing compiles broken):

1. sync & cloud pack plane: `CloudStickerRepository`, `StickerRepositoryImpl` push paths,
   `SyncManager` report surfaces (already `SyncReport`).
2. explore family (T3 data sources become `Result`-returning; façade pass-through),
3. auth (`AuthApiService`, `AuthManagerImpl`, `AuthSessionCoordinator`),
4. billing / legal / ai-usage / quota / history consumers as they surface.

`runCatching` in repositories is removed where a typed path replaces it (still allowed at
process boundaries like `AiJobRunner` worker step guards where each step already reports).

ViewModels map `DataError` → message using the existing `AppErrorCode` chains (no new string
tables), and surface `isLoading`/`error` states as they already do today.

### D7. What P3b is NOT

- No retry loops added at the HTTP client (classification helper only; sync loop already has
  bounded retry, and UI-facing calls fail fast per current UX).
- No behavioral change toRoom/local storage signatures (`OperationResult` deletes only).
- No WorkManager-style background sync rewrite: KMP; the app-scope async push from 5e1dda3
  remains (its `runCatching` gains an `AppLog` so failed pushes are not silent — noted during
  review of `5e1dda3`).
- No network policy/server contract changes.
- iOS: common code only; `klib` type-check (`compileKlibIos*`) is the gate we can run on
  Windows; linking/runtime verification remains a macOS follow-up.

---

## Staged execution (each stage ends green before next)

- **T1** D1 + D2 + D3 helper (no call sites switched): Result/DataError/AppCodec/ApiCall added;
  `AnimatedStickerValidator.Result` renamed; `SyncManagerImpl` retry classifier REPLACED early
  (typed fix, no split yet); `AiJobJson` folded into `ApiJson`.
- **T2** D4 sync split (internal only).
- **T3** D5 explore split (internal only).
- **T4** D6 Result migration family-by-family + HTTP clients step through `ApiCall`.

### Gates per stage (all must pass)

1. `:composeApp:compileCommonMainKotlinMetadata` — 0 errors / 0 warnings.
2. `:androidApp:assembleDebug` — 0 errors / 0 warnings.
3. `:composeApp:testAndroidHostTest` — all pass (currently 43 suites / 171 tests; the sync
   classifier change must keep or improve this total by adding regression tests for
  `Throwable.toDataError` classification only — no new test heroics).
4. Device smoke (USB + `adb reverse tcp:7845`, local API): app cold start, sync 200s observed,
   editor save completes fast, no FATAL/ANR.
5. Working state committed per stage; branch `audit/android-modernization`.

### Risks / mitigations

| Risk | Mitigation |
|------|------------|
| T4 call-site churn across 12+ ViewModels | Family-by-family commits; compile-verified per stage; no cross-family signature changes in one commit |
| Behavior drift in sync split | T2 is pure extraction (move, not rewrite); the only rewrite is the classifier, isolated and typed |
| JSON config drift breaks decode/encode | One added test: round-trip representative payloads (pack create/update AI payload, sync op payload) through `ApiJson` |
| Multipart logging regression | Explicit check in stage T1 gate: no request/response body bytes in `System.out`/`AppLog` |
| Ktor exceptions differ per platform | Classification is common over names available on both targets; `expect/actual` only where needed, documented in code |

---

## OPEN (for user review)

1. Confirm stage order T1→T4 with the classifier fix pulled forward into T1.
2. Naming: `domain/error` package for Result & DataError (as used in the skills' examples)
   vs. `domain/result` — propose `domain/error/Result.kt` + `DataError.kt`. OK?
3. `ExploreApiRepository` façade initially retained (D5) — OK per your earlier preference, or
   do you want call-site full migration included inside T3 (bigger blast radius)?
