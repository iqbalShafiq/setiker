# P3a — Execution plan: KMP module split (`:androidApp` + `:composeApp`)

Source spec: `docs/specs/p3a-kmp-module-split.md`
Branch: `audit/android-modernization`
Scope: Gradle/module topology only. No behavior, identity, iOS, asset, or resource-location change.

---

## Decisions taken (spec deviations are marked)

| # | Decision | Rationale |
| --- | --- | --- |
| D1 | **`BuildConfig` replacement = runtime install object, not a generated Kotlin file.** New `SetikerRuntimeConfig` (androidMain) with `isDebuggable` (from `ApplicationInfo.FLAG_DEBUGGABLE`) + `googleWebClientId`, installed as the first statement of `StickerApplication.onCreate`. `GOOGLE_WEB_CLIENT_ID` travels as an **Android string resource**: the library declares `google_web_client_id` (empty, `translatable="false"`), `:androidApp` overrides it with `resValue` + `buildFeatures { resValues = true }`. | No custom Gradle task, no generated source dir, no configuration-cache risk (spec risk #7 removed). App module owns the secret; library owns a safe default. `resValues` is disabled by default in AGP 9 (`android.defaults.buildfeatures.resvalues` true→false) and the documented way to re-enable it is `buildFeatures { resValues = true }` in the module that needs it. |
| D2 | `kotlin { android { ... } }` is used (not the deprecated `androidLibrary { }`). | Official: `androidLibrary {}` deprecated since AGP 9.1.0-alpha09; `android {}` since AGP 8.12. |
| D3 | `:androidApp` applies `com.android.application`, has **no Kotlin sources**, and sets `android { enableKotlin = false }`. | Official guidance for source-less/non-Kotlin subprojects under built-in Kotlin (removes the Kotlin compile task; fewer stdlib deps). |
| D4 | Library keeps `src/androidMain/res/**` and `src/androidMain/assets/**`; `androidResources { enable = true }` is required (resources are **not** enabled by default with the KMP Android library plugin). | Official plugin docs. |
| D5 | `libs.ui.tooling` moves from `androidMain.dependencies` to `dependencies { "androidRuntimeClasspath"(libs.ui.tooling) }` in `:composeApp`. | Official workaround #9: keeps `ui-tooling` off the compile classpath and out of the published API. |
| D6 | `gradle.properties`: drop `android.builtInKotlin=false` and `android.newDsl=false`. Version catalogue: add `androidKmpLibrary`, drop `androidLibrary`, `kotlinAndroid`, `kotlinCocoapods` (all three have zero consumers). | Goal of the split; no dead aliases. |
| D7 | `libs.ui.tooling.preview` stays in `commonMain`/`androidMain` (compile-time annotations). `libs.ui.tooling` is not declared in `:androidApp`. | The app module has no composables; AS renders previews from the library module runtime classpath (D5). |
| D8 | Keep `isMinifyEnabled = false`; keep `android.uniquePackageNames=false`, `android.dependency.useConstraints=true`, `android.r8.strictFullModeForKeepRules=false`, `android.generateSyncIssueWhenLibraryConstraintsAreEnabled=false`. | R8 work is deferred by user decision; the rest are explicit, non-deprecated choices. Out of P3a scope. |
| D9 | Fallback if `resValue` turns out to be deprecated/warning-producing under `android.newDsl=true`: transport the id as manifest `<meta-data android:name="com.setiker.app.GOOGLE_WEB_CLIENT_ID" android:value="${googleWebClientId}"/>` + `manifestPlaceholders`, read via `PackageManager.GET_META_DATA` in `StickerApplication`. | Keeps D1's properties (no codegen, app-owned) with a bulletproof transport. |

### Files touched (complete list)

Created:
- `androidApp/build.gradle.kts`
- `androidApp/src/main/AndroidManifest.xml`
- `composeApp/src/androidMain/kotlin/com/setiker/app/SetikerRuntimeConfig.kt`

Moved/renamed:
- `composeApp/src/androidUnitTest/**` → `composeApp/src/androidHostTest/**` (16 files)
- `composeApp/src/androidMain/AndroidManifest.xml` → `androidApp/src/main/AndroidManifest.xml` (content edited: 3 component names, permissions moved with the app declaration)

Edited:
- `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- `composeApp/build.gradle.kts` (full rewrite of the Android/app parts)
- `composeApp/src/androidMain/kotlin/StickerApplication.kt` (one install line)
- `composeApp/src/androidMain/kotlin/data/remote/ApiConfig.android.kt`
- `composeApp/src/androidMain/kotlin/data/auth/AndroidGoogleSignInGateway.kt`
- `composeApp/src/androidMain/res/values/strings.xml` (default `google_web_client_id`)

Deleted:
- `composeApp/src/main/res/drawable/ic_whatsapp.xml` (+ empty `composeApp/src/main/` tree; provably dead)

Not touched: `iosApp/**`, `iosMain/**`, `commonMain/**` (except nothing), `composeResources/**`, `assets/**`, any dependency version.

---

## Task 0 — Baseline (do not skip)

```powershell
cd C:\Users\iqbal.shafiq\SetikerProjects\setiker
.\gradlew.bat :composeApp:tasks --all *> build\tasks-before.txt
.\gradlew.bat :composeApp:testDebugUnitTest
```

Record in `docs/plans/p3a-log.md`:
- exact test task name and test count (baseline: 171 total / 56 android unit-test files),
- KSP task name and the existing `StickerDatabase_Impl.kt` path,
- the APK path (`composeApp/build/outputs/apk/debug/composeApp-debug.apk`).

Gate: baseline tests green, `build/tasks-before.txt` exists.

## Task 1 — Version catalogue + settings + root build

- `gradle/libs.versions.toml` `[plugins]`: add `androidKmpLibrary = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }`; remove `androidLibrary`, `kotlinAndroid`, `kotlinCocoapods`.
- `settings.gradle.kts`: add `include(":androidApp")`.
- `build.gradle.kts`: `alias(libs.plugins.androidKmpLibrary) apply false`; drop the three removed aliases.

Gate: `.\gradlew.bat projects` lists `:androidApp` and `:composeApp`.

## Task 2 — Create `:androidApp` (thin shell)

`androidApp/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.setiker.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.setiker.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Dev-only secret; empty when local.properties has no GOOGLE_WEB_CLIENT_ID.
        resValue("string", "google_web_client_id", googleWebClientId)
    }

    buildFeatures {
        // off by default in AGP 9 (android.defaults.buildfeatures.resvalues)
        resValues = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // No Kotlin sources in this module (built-in Kotlin is on by default in AGP 9).
    enableKotlin = false
}

val googleWebClientId: String = run {
    val localProperties = java.util.Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { stream -> load(stream) }
    }
    localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
        ?: (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?)
        ?: ""
}

dependencies {
    implementation(project(":composeApp"))
}
```

`androidApp/src/main/AndroidManifest.xml`: the current library manifest, unchanged except the three component names become fully qualified (`com.setiker.app.StickerApplication`, `com.setiker.app.MainActivity`, `com.setiker.app.StickerContentProvider`). `${applicationId}` authorities stay as they are.

Then delete `composeApp/src/androidMain/AndroidManifest.xml`.

Gate: run together with Task 3.

## Task 3 — `:composeApp` becomes a KMP Android **library**

Rewrite `composeApp/build.gradle.kts`:

- plugins: `kotlinMultiplatform`, `androidKmpLibrary`, `composeMultiplatform`, `composeCompiler`, `kotlinSerialization`, `ksp`.
- `kotlin { android { namespace = "com.setiker.app"; compileSdk = 37; minSdk = 24; compilerOptions { jvmTarget = JVM_21; freeCompilerArgs.add("-Xexpect-actual-classes") }; androidResources { enable = true }; withHostTest {} } }`.
- iOS targets/framework unchanged (`baseName = "ComposeApp"`, `isStatic = true`).
- `sourceSets`: `commonMain`/`androidMain`/`iosMain` dependency lists unchanged **except** `androidUnitTest.dependencies { mockk }` → `androidHostTest.dependencies { mockk }`, and `libs.ui.tooling` removed from `androidMain` (see D5).
- `dependencies { add("kspAndroid", libs.room.compiler); "androidRuntimeClasspath"(libs.ui.tooling) }`.
- Drop: top-level `android { }`, `manifest.srcFile`, `res.srcDirs`, `resources.srcDirs`, `applicationId`, `targetSdk`, `versionCode`, `versionName`, `buildTypes`, `packaging`, `buildConfigField`, `buildFeatures`, `compileOptions`, `lint { }` (move lint disable only if the KMP extension accepts it).
- On Windows `iosMain` cannot compile — the `-Xexpect-actual-classes` opt-in stays.

Gates (all must pass before Task 4):

```powershell
.\gradlew.bat :composeApp:tasks --all *> build\tasks-after.txt
.\gradlew.bat :composeApp:assemble
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :androidApp:processDebugMainManifest
```

Assertions:
- new task names recorded from `tasks-after.txt` (UNVERIFIED-1 closed),
- AAR produced (`composeApp/build/outputs/aar/*.aar`) and APK produced (`androidApp/build/outputs/apk/debug/androidApp-debug.apk`),
- merged manifest contains `com.setiker.app.StickerApplication`, `com.setiker.app.MainActivity` (MAIN/LAUNCHER), both provider authorities `com.setiker.app.fileprovider` / `com.setiker.app.stickercontentprovider`, the two `<queries>` entries,
- KSP ran: `StickerDatabase_Impl.kt` regenerated under `composeApp/build/generated/ksp/**` (UNVERIFIED-8 closed),
- APK contains `assets/models/u2net.onnx`, `assets/fonts/*.ttf`, `assets/composeResources/**` (UNVERIFIED-2 closed),
- `:androidApp:processDebugResources` resolves `@style/Theme.Setiker`, `@mipmap/ic_launcher`, `@string/app_name`, `@xml/network_security_config`, `@xml/file_paths` (UNVERIFIED-3 closed),
- APK has no `META-INF/AL2.0` / `META-INF/LGPL2.1` (UNVERIFIED-4 closed),
- **zero new deprecation warnings** about `android.newDsl` / `android.builtInKotlin` / legacy DSL (UNVERIFIED-11 closed),
- configuration cache: second `--configuration-cache` run reports "reused".

## Task 4 — `BuildConfig` replacement code (must land with Task 3 in one commit)

1. New `composeApp/src/androidMain/kotlin/com/setiker/app/SetikerRuntimeConfig.kt` (object with `isDebuggable`, `googleWebClientId`, `install(applicationInfo, googleWebClientId)`).
2. `composeApp/src/androidMain/res/values/strings.xml`: add `<string name="google_web_client_id" translatable="false"></string>`.
3. `StickerApplication.onCreate`: first statement after `super.onCreate()` → `SetikerRuntimeConfig.install(applicationInfo, getString(R.string.google_web_client_id))`.
4. `ApiConfig.android.kt`: `actual val isDebugLoggingEnabled: Boolean get() = SetikerRuntimeConfig.isDebuggable` (lazy getter — no init-order dependence).
5. `AndroidGoogleSignInGateway.kt`: replace the 3 `BuildConfig.GOOGLE_WEB_CLIENT_ID` usages with `SetikerRuntimeConfig.googleWebClientId`; drop the `BuildConfig` import.

Gates:

```powershell
.\gradlew.bat :composeApp:assemble :androidApp:assembleDebug
Select-String -Path .\composeApp\src\*\*\*.kt -Pattern "BuildConfig" -Recurse   # expect no hits
.\gradlew.bat :androidApp:assembleDebug "-PGOOGLE_WEB_CLIENT_ID=test-client-id-1234"
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\*\aapt2.exe" dump resources .\androidApp\build\outputs\apk\debug\androidApp-debug.apk | Select-String "google_web_client_id"
```

Assert: with `-PGOOGLE_WEB_CLIENT_ID=test-client-id-1234` the APK's `google_web_client_id` value is `test-client-id-1234` (proves the app-side value overrides the library default, i.e. the transport works). Rebuild afterwards without the property.

## Task 5 — Test source-set rename

`git mv composeApp/src/androidUnitTest composeApp/src/androidHostTest`; run the host-test task whose name Task 3 revealed (`testAndroidHostTest` expected).

Gate: same test count as Task 0 baseline; no source edits needed in the 16 moved files.

## Task 6 — Cleanup

- Delete `composeApp/src/main/res/drawable/ic_whatsapp.xml` and the empty `composeApp/src/main/` tree.
- Keep `assets/` in place.
- No leftover `import java.util.Properties` in `composeApp/build.gradle.kts` unless used.

Gate: `.\gradlew.bat :composeApp:assemble :androidApp:assembleDebug` still green; `git status` shows no stray files.

## Task 7 — Full verification (spec V1–V9, minus macOS-only V8)

- V1 configuration/task surface (done in Task 3),
- V2 library + app assemble (done),
- V3 KSP output (done),
- V4 manifest + resource merge (done),
- V5 APK contents (done),
- V6 unit tests (done),
- **V7 device**: `.\gradlew.bat :androidApp:installDebug`, `adb shell am start -n com.setiker.app/com.setiker.app.MainActivity`; smoke: launch → Home, pack list from Room, compose resources render, create pack → export to WhatsApp (`FileProvider` + `StickerContentProvider` authorities), deep links `setiker://ai/jobs`, background/AI notification icon. With the local API + `adb reverse tcp:7845 tcp:7845` available: additionally open Explore/Sync to exercise the network paths touched by P0–P2.
- V9 `:androidApp:assembleRelease` succeeds (minify off).
- V8 iOS: cannot run on Windows; assert `git diff --stat -- iosApp/` is empty.

## Task 8 — Commits + docs

Three commits, per spec granularity:
1. `build: add Android KMP library plugin alias, app module wiring and AGP 9 defaults` (Tasks 1–2 config bits),
2. `refactor: split app shell into :androidApp and convert :composeApp to a KMP Android library` (Tasks 3–4, atomic),
3. `test: move android unit tests to androidHostTest and clean dead res` (Tasks 5–6).

Then update `docs/specs/p3a-kmp-module-split.md` status → DONE with the resolved UNVERIFIED rows, and add a P3a row to the audit notes.

---

## Stop conditions

- Any gate fails twice with the same cause → stop, write the failure into `docs/plans/p3a-log.md`, and report instead of improvising a redesign.
- If resources do not merge (V4/V5), the `:androidApp` shell decision needs revisiting (do not "fix" by copying `res/` into `:androidApp`).

---

## RESULTS (executed)

Baseline (`build/tasks-before.txt`, `build/tests-before.txt`): `:composeApp:testDebugUnitTest` = 171 tests,
KSP `kspDebugKotlinAndroid` → `composeApp/build/generated/ksp/android/androidDebug/kotlin/data/local/database/StickerDatabase_Impl.kt`,
APK `composeApp/build/outputs/apk/debug/composeApp-debug.apk` (371 MB), plus a pre-existing
`WARNING: API 'unitTestVariants' is obsolete` from the legacy DSL.

| Gate | Result |
| --- | --- |
| Task 1 `gradlew projects` | PASS — `:androidApp` + `:composeApp` (no `newDsl`/`builtInKotlin` deprecation warnings) |
| Task 3 task surface (`tasks-after.txt`) | PASS — AAR: `assembleAndroidMain` / `bundleAndroidMainAar`; tests: `testAndroidHostTest`; KSP: `kspAndroidMain` |
| Task 3 `:androidApp:assembleDebug` | PASS — 0 errors, 0 warnings, 4m; APK 369.8 MB |
| V2 `:composeApp:assembleAndroidMain` | PASS — `composeApp/build/outputs/aar/composeApp.aar` (201 MB) |
| V3 KSP/Room | PASS — `StickerDatabase_Impl.kt` under `composeApp/build/generated/ksp/android/androidMain/kotlin/...` |
| V4 manifest merge | PASS — `package="com.setiker.app"`, `com.setiker.app.StickerApplication`, `com.setiker.app.MainActivity` (MAIN/LAUNCHER), authorities `com.setiker.app.fileprovider` / `com.setiker.app.stickercontentprovider`, both WhatsApp `<queries>` |
| V5 APK contents | PASS — `assets/models/u2net.onnx`, 3 fonts, 33 `assets/composeResources/**` entries; no `META-INF/AL2.0` / `LGPL2.1` |
| Task 4 resource override | PASS — built with `-PGOOGLE_WEB_CLIENT_ID=test-client-id-1234`; `aapt2 dump resources` shows `string/google_web_client_id = "test-client-id-1234"` (app overrides the library default) |
| V6 unit tests | PASS — `testAndroidHostTest`: 43 suites / 171 tests / 0 failures (identical to baseline) |
| V1 configuration cache | PASS — second `installDebug` run: "Configuration cache entry reused" |
| V7 device smoke | PASS — launch → Home (pack list + 16 stickers from Room), compose resources render, `setiker://ai/jobs` deep link opens AI Jobs & Drafts with live data, providers registered (sticker provider enforces `com.whatsapp.sticker.READ`, so the `${applicationId}` authorities are wired), identity intact (`versionCode=1`, `versionName=1.0`, `minSdk=24`, `targetSdk=37`), no FATAL in logcat |
| V8 iOS | NOT RUNNABLE (Windows). `git diff --stat -- iosApp/` empty; framework name/`baseName`/target set and `:composeApp:embedAndSignAppleFrameworkForXcode` untouched |
| V9 release | PASS — `:androidApp:assembleRelease` → 360.4 MB unsigned APK, no R8 rules needed |

### UNVERIFIED items — closed

1, 2, 3, 4, 5, 6, 8, 9, 10, 11 closed by the gates above. 7 (`ui-tooling` reachability) resolved by D5 per the
official KMP workaround (`androidRuntimeClasspath`), so the app APK intentionally does not carry `ui-tooling`.
12 (iOS) remains host-limited, as expected.

### Deviation found during execution — pre-existing, NOT caused by the split

`:composeApp:assemble` fails on `compileCommonMainKotlinMetadata`, and it **also fails on the pre-split baseline**
(verified by stashing the work and re-running: same failure). `commonMain` contains JVM/Android-only APIs:

| File | Issue |
| --- | --- |
| `presentation/navigation/AppNavigation.kt` | 24 × `arguments?.getString/getInt/getLong/getFloat` (Android `Bundle` API) — **fixed in this work** via `SavedState.read { getStringOrNull(...) }` |
| `data/aijob/AiJobRunner.kt:461,467` | `OutOfMemoryError` (JVM-only) |
| `presentation/components/EmojiPickerBottomSheet.kt:50-56` | deprecated `String(CharArray)` + `Character.toChars` (JVM-only) |
| `presentation/components/KeyboardAwareScroll.kt:7,25,37,43` | `Modifier.imeNestedScroll()` (Android-only) |
| `presentation/editor/EditorScreenRoot.kt:44`, `EditorViewModel.kt:869-946` | `android.*` types in commonMain |

Consequence: the metadata klib compilation and the **iOS target cannot compile** today. Android is unaffected
(the APK path never compiles metadata). Fixing the remaining four files needs real refactoring
(expect/actual or multiplatform replacements), so it is tracked as a separate task (P3h) instead of being
smuggled into a topology-only change. P3a's Android gates were therefore scoped to `:androidApp:assembleDebug`,
`:composeApp:assembleAndroidMain` and `:composeApp:testAndroidHostTest`.

### Notes / follow-ups

- `resValue` + `buildFeatures { resValues = true }` produced no deprecation warning on AGP 9.4.1 with `android.newDsl=true`, so D9's fallback was not needed.
- `android.uniquePackageNames=false` and `android.dependency.useConstraints=true` are kept (D8); the namespaces are already unique, so `uniquePackageNames` could be dropped later.
- `lint { disable += "UnsafeOptInUsageError" }` inside `kotlin { android { ... } }` is accepted by AGP 9.4.1.
- `composeApp/src/main/` (dead `ic_whatsapp.xml`) deleted; `src/commonMain/resources` wiring dropped (directory never existed).
- Local dev against the local API (port 7845) still needs a baseUrl + cleartext override; proposed separately.
