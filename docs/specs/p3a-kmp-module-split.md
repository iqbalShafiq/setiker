# P3a — KMP Module Split: `:androidApp` + Android KMP Library `:composeApp`

Status: DONE — implemented on `audit/android-modernization`. Gate results and the deviations
from this spec (resource-based BuildConfig transport, no codegen) are recorded in
`docs/plans/2026-10-16-p3a-kmp-module-split.md`.
Scope: Gradle/module topology only. No application behavior change.
Target toolchain: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.21, KSP 2.3.12, Compose Multiplatform 1.12.1, Room 2.8.5, JDK 21.

---

## Context

Today Setiker has a single Gradle module, `:composeApp`, that simultaneously:

- applies `org.jetbrains.kotlin.multiplatform`,
- applies `com.android.application`,
- declares `iosArm64` / `iosSimulatorArm64` targets that produce the `ComposeApp.framework` consumed by `iosApp/iosApp.xcodeproj`,
- and hosts all Android application code (`MainActivity`, `StickerApplication`, `StickerContentProvider`, Room/KSP, Android resources, Android assets).

Two opt-outs are currently required to keep that arrangement working on AGP 9 (`gradle.properties`):

```properties
android.builtInKotlin=false
android.newDsl=false
```

These exist only because AGP 9 deprecated/removed the "KMP + `com.android.application` in one module" shape. The supported AGP 9 shape is:

- an Android **application** module that depends on
- a KMP module whose Android target is a **library**, produced by the `com.android.kotlin.multiplatform.library` plugin.

This spec defines that split with the smallest possible blast radius:

- **Zero iOS/Xcode change** — the framework keeps the same base name (`ComposeApp`), the same Gradle task (`:composeApp:embedAndSignAppleFrameworkForXcode`), and the same module name, so `import ComposeApp` in Swift is untouched.
- **Zero `applicationId` change** — `com.setiker.app` is hardcoded in `commonMain` billing repositories (`BillingRepositoryImpl.kt`, `BillingApiRepository.kt`) and must remain.
- **Maximum code retention in the shared module** — anything that only needs an Android `Context` stays in the KMP library. Almost all Android code is "library-shaped" already.

References (official):

- Android KMP plugin — <https://developer.android.com/kotlin/multiplatform/plugin>
- AGP 9.0 release notes (KMP plugin, built-in Kotlin, new DSL defaults) — <https://developer.android.com/build/releases/agp-9-0-0-release-notes>
- AGP/DSL roadmap (new DSL, `android.newDsl`) — <https://developer.android.com/build/releases/gradle-plugin-roadmap>
- Kotlin recommended KMP project structure — <https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html>
- JetBrains AGP 9 migration skill (app-split guidance) — <https://github.com/Kotlin/kotlin-agent-skills/blob/main/skills/kotlin-tooling-agp9-migration/references/MIGRATION-APP-SPLIT.md>
- KSP + KMP Android library wiring issue — <https://github.com/google/ksp/issues/2476> (workaround `fixKspAndroidMainProcessorClasspath`; reported fixed from KSP 2.3.6; project is on 2.3.12)

Every statement below that could not be confirmed against the local repo, the local AGP jars, or the pages above is listed in **UNVERIFIED** with the command that confirms it.

---

## Decision

Adopt the **thin application shell** shape:

1. Add a new, essentially source-less Android application module `:androidApp` (namespace `com.setiker.android`) that owns only: the app `AndroidManifest.xml`, `applicationId`, `versionCode`, `versionName`, `targetSdk`, `compileSdk`, release `buildType`, and `packaging` excludes. It has exactly one dependency: `implementation(project(":composeApp"))`.
2. Convert `:composeApp` in place to a **KMP Android library** via `com.android.kotlin.multiplatform.library`, keeping the module name `:composeApp` and its iOS targets/framework unchanged.
3. Remove `android.newDsl=false` and `android.builtInKotlin=false` from `gradle.properties`, moving onto AGP 9 defaults.
4. Replace the single `BuildConfig` consumer surface (`GOOGLE_WEB_CLIENT_ID`, `DEBUG`) with a generated Kotlin constants file plus a runtime `FLAG_DEBUGGABLE` check, because the KMP library variant has no `BuildConfig`.

### Why `:composeApp` is **not** renamed

The Compose-resources plugin derives its generated package from `project.group` + `project.name`:

```
setiker.composeapp.generated.resources   // = rootProject.name.lowercase() + "." + project.name.lowercase() + ".generated.resources"
```

Confirmed in the current build output at
`composeApp/build/generated/compose/resourceGenerator/kotlin/commonResClass/setiker/composeapp/generated/resources/Res.kt`.

Renaming the module to e.g. `:shared` would change that package and invalidate every `setiker.composeapp.generated.resources.Res` import. Keeping the name also keeps `:composeApp:embedAndSignAppleFrameworkForXcode` and `baseName = "ComposeApp"` valid. **Decision: do not rename.**

---

## Target structure

### Module graph

```
Setiker/ (root, no plugins applied)
├── :androidApp            com.android.application              ← new, thin shell
│     └── implementation(project(":composeApp"))
└── :composeApp            org.jetbrains.kotlin.multiplatform
      ├── com.android.kotlin.multiplatform.library  (android target → AAR)
      ├── org.jetbrains.compose / kotlin.plugin.compose
      ├── com.google.devtools.ksp
      ├── iosArm64 + iosSimulatorArm64  → ComposeApp.framework (static)
      ├── src/commonMain        (shared UI, domain, data, Room entities/DAOs)
      ├── src/androidMain       (Activity, Application, providers, KSP-generated room impl, res/, assets/)
      ├── src/iosMain
      ├── src/commonTest
      └── src/androidHostTest   ← renamed from src/androidUnitTest
```

Dependency direction is strictly `:androidApp → :composeApp`. Nothing depends on `:androidApp`, and the library never sees the app module (this is why `BuildConfig` from the app cannot be used by the library).

### Placement table (and explicit justification)

| Artifact / concern | Current location | New location | Why |
| --- | --- | --- | --- |
| `AndroidManifest.xml` app declarations (permissions, `<application>`, activity, providers, `<queries>`) | `composeApp/src/androidMain/AndroidManifest.xml` | **`androidApp/src/main/AndroidManifest.xml`** | Manifest merge rules, `applicationId` placeholders, launcher intent, and exported providers are application-level concepts. A library manifest cannot declare `MAIN`/`LAUNCHER`. |
| `applicationId`, `versionCode`, `versionName`, `targetSdk` | `composeApp/build.gradle.kts` `defaultConfig` | **`:androidApp`** `defaultConfig` | App-only concepts; not settable on the KMP library extension. |
| `compileSdk`, `minSdk`, `namespace` | `composeApp` | **both modules** | `:composeApp` needs them to compile against Android APIs; `:androidApp` needs them to build the APK. `namespace` must differ (`com.setiker.app` vs `com.setiker.android`). |
| `buildTypes { release { isMinifyEnabled = false } }` | `composeApp` | **`:androidApp`** | Build types belong to the application. |
| `packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }` | `composeApp` | **`:androidApp`** | APK-level packaging. |
| `MainActivity`, `MainActivityHolder`, `StickerApplication`, `StickerContentProvider`, `WhatsAppStickerLauncher` | `androidMain` | **stay in `:composeApp`** | They need only `Context`/`Activity`. Moving them would force widening `internal object ForegroundActivityProvider`, re-declaring every `implementation` dependency in `:androidApp`, and dragging the Compose compiler into the app module. |
| Room `@Database`/entities/DAOs + `DatabaseMigrations` + `StickerDatabase_Impl` (KSP) | `commonMain` + KSP `androidMain` output | **stay in `:composeApp`** | Room code is shared-module code; only the KSP processor runs per-target. |
| WorkManager workers (`SyncWorker`, AI job workers), billing, Credential Manager/Google Sign-In, storage implementations | `androidMain` (KSP/billing in common where applicable) | **stay in `:composeApp`** | Same reason; they are `implementation` details of the shared module. |
| All Android resources `res/` (`mipmap/ic_launcher*`, `values/themes.xml`, `values-night/themes.xml`, `values/strings.xml`, `values/colors.xml`, `xml/file_paths.xml`, `xml/network_security_config.xml`, `drawable*`) | `composeApp/src/androidMain/res` | **stay in `:composeApp`** | Referenced from Kotlin (`AiNotificationHelper` uses `R.mipmap.ic_launcher`) *and* from the app manifest. Library resources merge into the APK, so both keep working. Moving them would create two divergent resource sets. |
| Android assets `assets/models/u2net.onnx`, `assets/fonts/*.ttf` | `composeApp/src/androidMain/assets` | **stay in `:composeApp`** | Loaded via `context.assets.open("models/u2net.onnx")` and `Typeface.createFromAsset`; AAR assets merge into the APK. |
| Compose resources `src/commonMain/composeResources` | `composeApp` | **stay in `:composeApp`** | Generated `Res` class + `assets/composeResources/**` must be produced by the library that owns the `composeMultiplatform` plugin. |
| `src/main/res/drawable/ic_whatsapp.xml` (dead dir) | `composeApp/src/main` | **delete** | Unwired: `android.sourceSets["main"].res.srcDirs("src/androidMain/res")` replaced the default `src/main/res`, so this file was never in the build. |
| `src/commonMain/resources` wiring | `android.sourceSets["main"].resources.srcDirs(...)` | **drop the line** | The directory does not exist; the wiring is a no-op. |
| `BuildConfig.GOOGLE_WEB_CLIENT_ID` | `composeApp` `buildConfigField` | **generated `SetikerBuildConfig.kt` in `:composeApp`** | The library variant has no `BuildConfig`; the app's `BuildConfig` is unreachable from a library. Source of truth (`local.properties` / project property) unchanged. |
| `BuildConfig.DEBUG` | `composeApp` `BuildConfig` | **runtime `ApplicationInfo.FLAG_DEBUGGABLE`** | Library has no build types, so `DEBUG` is meaningless there; the app's debuggable flag is the correct runtime signal. |
| `kotlin { androidTarget { ... } }` | `composeApp` | **`kotlin { android { ... } }`** | New KMP Android library DSL. |

---

## Step-by-step migration

Each step ends with a validation gate. Do not proceed past a failing gate.

### Step 0 — Pre-flight baseline (record what "working" means)

```powershell
.\gradlew.bat clean
.\gradlew.bat :composeApp:assembleDebug
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :composeApp:tasks --all > build\tasks-before.txt
```

Capture for later comparison:

```powershell
# merged manifest + assets of the current APK
.\gradlew.bat :composeApp:processDebugMainManifest
Get-Content .\composeApp\build\intermediates\merged_manifest\debug\processDebugMainManifest\AndroidManifest.xml
# assets currently packaged
tar -tf .\composeApp\build\outputs\apk\debug\composeApp-debug.apk | Select-String "assets/"
```

Gate: baseline APK exists at `composeApp/build/outputs/apk/debug/composeApp-debug.apk` and unit tests pass. Keep `build/tasks-before.txt` — it is the reference for every task-name assertion in the verification plan.

### Step 1 — Version catalogue

`gradle/libs.versions.toml`, `[plugins]` section: add the KMP Android library plugin and delete the two aliases that no other module uses.

```toml
[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidKmpLibrary  = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "compose-compiler" }
kotlinCocoapods = { id = "org.jetbrains.kotlin.native.cocoapods", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.3.12" }
```

Removed: `androidLibrary` (`com.android.library`) and `kotlinAndroid` (`org.jetbrains.kotlin.android`) — after Step 6 no module applies them. `kotlinCocoapods` has no consumer either; leave it or remove it, but that is unrelated cleanup.

Gate: `.\gradlew.bat help` resolves the catalogue.

### Step 2 — `settings.gradle.kts`

```kotlin
rootProject.name = "Setiker"
include(":composeApp")
include(":androidApp")
```

No typesafe project accessors are enabled in this build, so `:androidApp` must reference the library with `project(":composeApp")` (not `projects.composeApp`).

Gate: `.\gradlew.bat projects` lists `:androidApp` and `:composeApp`.

### Step 3 — Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}
```

Differences from today: `androidKmpLibrary` added; `androidLibrary` and `kotlinAndroid` removed (they are gone from the catalogue in Step 1).

Gate: `.\gradlew.bat help`.

### Step 4 — `gradle.properties`

```diff
 #Android
 android.nonTransitiveRClass=true
 android.useAndroidX=true
 android.uniquePackageNames=false
 android.dependency.useConstraints=true
 android.r8.strictFullModeForKeepRules=false
 android.generateSyncIssueWhenLibraryConstraintsAreEnabled=false
-android.builtInKotlin=false
-android.newDsl=false
```

Notes:

- Both flags were opt-outs that existed only to keep KMP + `com.android.application` in one module. With the split they are no longer needed, and removing them puts the build on AGP 9 defaults (new DSL on, built-in Kotlin on).
- `android.uniquePackageNames=false` can stay. After the split the namespaces are genuinely unique (`com.setiker.app` vs `com.setiker.android`), so it is optional; keeping it minimizes the diff.
- `org.gradle.configuration-cache=true` stays. It interacts with Step 8: the generated-source task must capture plain values/providers and must not touch `project` in `doLast`.
- `android.builtInKotlin=true` (the new default) forbids applying `org.jetbrains.kotlin.android` in the same module — which is exactly why `:androidApp` must not apply it (Step 5) and why `:composeApp` uses the KMP plugin, not `kotlin.android`.

Gate: `.\gradlew.bat :composeApp:help` still configures (the plugin change lands in Step 6, so this gate is really "properties file parses").

### Step 5 — Create `:androidApp`

Create `androidApp/build.gradle.kts`:

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

    // This module has no Kotlin sources of its own.
    // See UNVERIFIED-6 if this property is rejected by the AGP version in use.
    enableKotlin = false
}

dependencies {
    implementation(project(":composeApp"))
}
```

`androidApp/src/main/AndroidManifest.xml` — the current library manifest, relocated, with fully qualified component names (because the app namespace is `com.setiker.android` while the classes live in `com.setiker.app`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Internet permission for image loading (Coil) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <!-- Photo/video access uses system picker (SAF); no broad media permissions required -->
    <!-- Permission to query if WhatsApp is installed -->
    <queries>
        <package android:name="com.whatsapp" />
        <package android:name="com.whatsapp.w4b" />
    </queries>

    <application
        android:name="com.setiker.app.StickerApplication"
        android:largeHeap="true"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:networkSecurityConfig="@xml/network_security_config"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:usesCleartextTraffic="true"
        android:supportsRtl="true"
        android:theme="@style/Theme.Setiker">

        <activity
            android:name="com.setiker.app.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Setiker.Splash"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter android:autoVerify="false">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="http" android:host="13.251.98.162" android:pathPrefix="/api/v1/share/pack/" />
                <data android:scheme="http" android:host="13.251.98.162" android:pathPrefix="/api/v1/share/sticker/" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="setiker" android:host="ai" android:pathPrefix="/draft" />
                <data android:scheme="setiker" android:host="ai" android:pathPrefix="/job" />
                <data android:scheme="setiker" android:host="ai" android:path="/jobs" />
            </intent-filter>
        </activity>

        <!-- FileProvider for sharing sticker files -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <!-- WhatsApp Sticker ContentProvider -->
        <provider
            android:name="com.setiker.app.StickerContentProvider"
            android:authorities="${applicationId}.stickercontentprovider"
            android:enabled="true"
            android:exported="true"
            android:readPermission="com.whatsapp.sticker.READ" />

        <!-- WorkManager foreground worker (required when using FOREGROUND_SERVICE_TYPE_DATA_SYNC) -->
        <service
            android:name="androidx.work.impl.foreground.SystemForegroundService"
            android:exported="false"
            android:foregroundServiceType="dataSync"
            tools:node="merge" />

    </application>
</manifest>
```

Three changes versus the current file, and nothing else:

1. `android:name=".StickerApplication"` → `com.setiker.app.StickerApplication`.
2. `android:name=".MainActivity"` → `com.setiker.app.MainActivity`.
3. `android:name=".StickerContentProvider"` → `com.setiker.app.StickerContentProvider`.

`${applicationId}.fileprovider` / `${applicationId}.stickercontentprovider` are **unchanged** and remain correct: `${applicationId}` expands to `com.setiker.app`, and all runtime authority lookups already derive from `context.packageName` / `activity.packageName` rather than `BuildConfig.APPLICATION_ID`.

Then delete `composeApp/src/androidMain/AndroidManifest.xml`. The library does not need a manifest (AGP synthesizes an empty one if none is present).

Gate: `.\gradlew.bat :androidApp:processDebugMainManifest` succeeds. This will fail until Step 6 lands, so run it after Step 6.

### Step 6 — Rewrite `:composeApp/build.gradle.kts`

```kotlin
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)      // com.android.kotlin.multiplatform.library
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.setiker.app"
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        // Required: the Android resources in src/androidMain/res must be packaged
        // into the AAR so the app manifest can reference @style/@mipmap/@string.
        androidResources {
            enable = true
        }

        withHostTest {}                            // produces the androidHostTest source set

        lint {
            disable += "UnsafeOptInUsageError"
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // ...unchanged from today...
        }

        androidMain.dependencies {
            // ...unchanged from today...
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidHostTest.dependencies {
            implementation(libs.mockk)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
}

// ---------------------------------------------------------------------------
// BuildConfig replacement (the library variant has no BuildConfig).
// Source of truth is unchanged: local.properties or -PGOOGLE_WEB_CLIENT_ID.
// ---------------------------------------------------------------------------
val googleWebClientId: String = run {
    val localProperties = java.util.Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { stream -> load(stream) }
    }
    localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
        ?: (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?)
        ?: ""
}

val setikerBuildConfigOutputDir = layout.buildDirectory.dir("generated/setikerBuildConfig/kotlin")

val generateSetikerBuildConfig = tasks.register("generateSetikerBuildConfig") {
    val clientId = googleWebClientId
    val outputDir = setikerBuildConfigOutputDir
    inputs.property("googleWebClientId", clientId)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/setiker/app")
        packageDir.mkdirs()
        packageDir.resolve("SetikerBuildConfig.kt").writeText(
            buildString {
                appendLine("package com.setiker.app")
                appendLine()
                appendLine("internal object SetikerBuildConfig {")
                appendLine("    const val GOOGLE_WEB_CLIENT_ID: String = \"$clientId\"")
                appendLine("}")
            }
        )
    }
}

kotlin.sourceSets.named("androidMain") {
    kotlin.srcDir(generateSetikerBuildConfig)
}
```

What changed, relative to today:

| Today | New |
| --- | --- |
| `alias(libs.plugins.androidApplication)` | `alias(libs.plugins.androidKmpLibrary)` |
| `kotlin { androidTarget { compilerOptions { ... } } }` | `kotlin { android { compilerOptions { ... } ... } }` |
| top-level `android { namespace/compileSdk/sourceSets/defaultConfig/buildFeatures/buildTypes/compileOptions/lint }` | moved into `kotlin { android { ... } }`, minus app-only concerns |
| `androidUnitTest.dependencies` | `androidHostTest.dependencies` |
| `applicationId`, `targetSdk`, `versionCode`, `versionName`, `buildTypes`, `packaging`, `buildConfigField` | removed → `:androidApp` (or replaced by the generated constants file) |
| `buildFeatures { compose = true; buildConfig = true }` | removed (Compose is enabled by the Compose plugin; buildConfig does not exist here) |
| `compileOptions { sourceCompatibility/targetCompatibility = VERSION_21 }` | removed — the KMP library target uses `compilerOptions.jvmTarget`; the app module's Java level comes from AGP defaults |
| `sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")` | removed — manifest relocated to `:androidApp` |
| `sourceSets["main"].res.srcDirs("src/androidMain/res")` | removed pending UNVERIFIED-3 |
| `sourceSets["main"].resources.srcDirs("src/commonMain/resources")` | removed (directory does not exist) |

Keep `androidResources { enable = true }` — this is what makes `res/` merge into the APK. Also confirm asset wiring (UNVERIFIED-2).

Gate:

```powershell
.\gradlew.bat :composeApp:tasks --all > build\tasks-after.txt
.\gradlew.bat :composeApp:assemble        # builds AAR + Compose resources
.\gradlew.bat :androidApp:assembleDebug
```

Compare `tasks-before.txt` against `tasks-after.txt` and update the verification plan below with the real names.

### Step 7 — BuildConfig replacement in Kotlin

New file `composeApp/src/androidMain/kotlin/com/setiker/app/SetikerRuntimeFlags.kt`:

```kotlin
package com.setiker.app

import android.content.pm.ApplicationInfo

/**
 * Runtime replacement for the old `BuildConfig.DEBUG`.
 * The KMP Android library produces a single variant, so the debug flag must come
 * from the installed application's ApplicationInfo instead.
 */
object SetikerRuntimeFlags {
    @Volatile
    var isDebuggable: Boolean = false
        private set

    fun install(applicationInfo: ApplicationInfo) {
        isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
```

`composeApp/src/androidMain/kotlin/StickerApplication.kt` — install the flag first, before Koin starts:

```kotlin
override fun onCreate() {
    super.onCreate()
    SetikerRuntimeFlags.install(applicationInfo)      // <- new line
    ForegroundActivityProvider.install(this)
    // ...unchanged...
}
```

`composeApp/src/androidMain/kotlin/data/remote/ApiConfig.android.kt`:

```kotlin
package data.remote

import com.setiker.app.SetikerRuntimeFlags

actual object ApiConfig {
    actual val baseUrl: String = "http://13.251.98.162:8080"
    actual val isDebugLoggingEnabled: Boolean get() = SetikerRuntimeFlags.isDebuggable
}
```

Note the `get()`: the previous eager `val` read a compile-time constant, but the replacement is only set in `StickerApplication.onCreate`. A lazy getter removes any initialization-order dependence.

`composeApp/src/androidMain/kotlin/data/auth/AndroidGoogleSignInGateway.kt` — swap the import and the three usages:

```kotlin
import com.setiker.app.SetikerBuildConfig
// ...
override fun isAvailable(): Boolean = SetikerBuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()
// ...
val serverClientId = SetikerBuildConfig.GOOGLE_WEB_CLIENT_ID
```

Gate: `.\gradlew.bat :composeApp:compileDebugKotlinAndroid` (name per UNVERIFIED-1) — zero references to `com.setiker.app.BuildConfig` may remain:

```powershell
Select-String -Path .\composeApp\src\**\*.kt -Pattern "BuildConfig" -Recurse
```

### Step 8 — Test source-set rename

```powershell
git mv composeApp/src/androidUnitTest composeApp/src/androidHostTest
```

(or a plain move if the tree is not tracked). The 16 test files listed under `androidHostTest/kotlin/presentation/**` and `androidHostTest/kotlin/data/**` need no source edits — they only depend on `mockk`, which moved with the source set in Step 6.

Gate: `.\gradlew.bat :composeApp:testDebugUnitTest` (name per UNVERIFIED-1) runs the same tests as the Step 0 baseline.

### Step 9 — Cleanup

- Delete `composeApp/src/main/res/drawable/ic_whatsapp.xml` (and the now-empty `composeApp/src/main/` tree). It was never wired into the build.
- Keep `composeApp/src/androidMain/assets/**` where it is (see UNVERIFIED-2 for the wiring check).
- `composeApp/build.gradle.kts`: delete the now-unused `import java.util.Properties` if the inline `java.util.Properties()` form is used, or keep the import and use the short form consistently.

### Incremental ordering (do not reorder)

1. **Step 1 → 4** (catalogue, settings, root, properties): pure configuration, no code move. Cheap to revert.
2. **Step 5** (create `:androidApp`) *before* Step 6: the app module can exist while the library still applies `com.android.application`; nothing resolves until Step 6, which is fine because no gate is run in between. Creating it first means Step 6's gate (`:androidApp:processDebugMainManifest`) is runnable immediately.
3. **Step 6 + 7** must land together. Step 6 removes `BuildConfig`, Step 7 removes its last two consumers. Splitting them leaves the module uncompilable.
4. **Step 6** also deletes the library manifest (in Step 5). Manifest deletion and the DSL change are one atomic commit.
5. **Step 8** (test rename) after Step 6, because the source-set name `androidHostTest` only exists once `withHostTest {}` is configured.
6. **Step 9** last.

Recommended commit granularity: one commit for Steps 1–4 (config), one for Steps 5–7 (the split, must be atomic), one for Steps 8–9 (tests + cleanup).

---

## Verification plan

Run from the repo root, in this order.

### V1 — Configuration and task surface

```powershell
.\gradlew.bat --configuration-cache :composeApp:tasks --all > build\tasks-after.txt
.\gradlew.bat --configuration-cache :androidApp:tasks --all
```

Expected: no `android.newDsl` / `android.builtInKotlin` deprecation warnings; the AGP 9 KMP warning about `com.android.application` + KMP is gone. Diff `build\tasks-after.txt` vs `build\tasks-before.txt` and record the real names of the compile/test/ksp tasks (UNVERIFIED-1).

### V2 — Library + app compile

```powershell
.\gradlew.bat :composeApp:assemble
.\gradlew.bat :androidApp:assembleDebug
```

Expected: `composeApp/build/outputs/aar/composeApp-debug.aar` (or `composeApp.aar`) and `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

### V3 — KSP / Room output

```powershell
.\gradlew.bat :composeApp:kspDebugKotlinAndroid
Get-ChildItem -Recurse -Filter "StickerDatabase_Impl.kt" .\composeApp\build\generated\ksp
```

Expected: exactly the file that exists today under `.../ksp/android/androidDebug/kotlin/data/local/database/StickerDatabase_Impl.kt`. Note: the project configures **only** `kspAndroid`; there is no `kspIosArm64`/`kspIosSimulatorArm64`. iOS Room generation is pre-existing and out of scope — an iOS-side Room failure is **not** caused by this split.

### V4 — Manifest and resource merge

```powershell
.\gradlew.bat :androidApp:processDebugMainManifest
Get-Content .\androidApp\build\intermediates\merged_manifest\debug\processDebugMainManifest\AndroidManifest.xml
```

Assert all of:

- `package` / `android:name` of application is `com.setiker.app.StickerApplication`
- launcher activity is `com.setiker.app.MainActivity` with `MAIN`/`LAUNCHER`
- providers present with authorities `com.setiker.app.fileprovider` and `com.setiker.app.stickercontentprovider`
- `<queries>` for `com.whatsapp` and `com.whatsapp.w4b` present
- library-declared resources resolved (e.g. `android:theme="@style/Theme.Setiker"` still an unresolved reference at manifest level is normal; the resource must exist in the merged resource table)

```powershell
.\gradlew.bat :androidApp:processDebugResources
```

### V5 — APK contents (assets, compose resources, res)

```powershell
tar -tf .\androidApp\build\outputs\apk\debug\androidApp-debug.apk | Select-String "assets/"
```

Assert presence of:

- `assets/models/u2net.onnx`
- `assets/fonts/luckiest_guy_regular.ttf`, `assets/fonts/fredoka_bold.ttf`, `assets/fonts/bungee_regular.ttf`
- `assets/composeResources/**`

The `u2net.onnx` entry also implicitly verifies UNVERIFIED-2.

### V6 — Unit tests

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest
```

Expected: the same tests as the Step 0 baseline (16 test files under `androidHostTest`), no reduction.

### V7 — Device install and smoke test

```powershell
.\gradlew.bat :androidApp:installDebug
adb -s 2207117BPG shell am start -n com.setiker.app/com.setiker.app.MainActivity
adb -s 2207117BPG shell dumpsys package com.setiker.app | Select-String "versionCode|targetSdk|minSdk"
```

Smoke checklist on device `2207117BPG`:

1. App launches from the launcher and reaches the Home screen.
2. Database opens (Room) — navigate to a screen that reads packs; no `StickerDatabase_Impl` / `IllegalStateException` in logcat.
3. Compose resources render — icons/strings/images that come from `composeResources`.
4. A sticker pack can be created and exported to WhatsApp (exercises `StickerContentProvider` + `FileProvider`, i.e. both `${applicationId}` authorities).
5. Google Sign-In availability reflects `GOOGLE_WEB_CLIENT_ID` (button hidden/shown as configured).
6. Background sync / AI job notification renders the launcher icon (exercises `R.mipmap.ic_launcher` from the library).
7. Apply a sticker / share link (`setiker://ai/...` and `http://13.251.98.162/api/v1/share/...`) — deep links still resolve to `com.setiker.app.MainActivity`.

### V8 — iOS regression (macOS only, cannot be run on this host)

```powershell
.\gradlew.bat :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Expected: `ComposeApp.framework` produced with the same base name. `iosApp/iosApp.xcodeproj/project.pbxproj` must be **unmodified** (`./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` still valid). Verify with:

```powershell
git diff --stat -- iosApp/
```

Expected: empty.

### V9 — Release sanity

```powershell
.\gradlew.bat :androidApp:assembleRelease
```

`isMinifyEnabled = false` per the current configuration, so this should succeed without R8 keep rules.

---

## Risks and unknowns

### Behavioural risks

1. **Library resources must actually merge** (manifest references `@style/Theme.Setiker`, `@mipmap/ic_launcher`, `@string/app_name`, `@xml/network_security_config`, `@xml/file_paths` from `:composeApp/src/androidMain/res`). If `androidResources { enable = true }` is missing or wrong for this plugin version, `:androidApp:processDebugResources` fails with unresolved references. Mitigation: keep `res/` in the library (already decided) and treat V5 as the hard gate.
2. **Init-order regression on the debug flag.** `BuildConfig.DEBUG` was a compile-time constant; the replacement is set in `StickerApplication.onCreate`. If any code reads `ApiConfig.isDebugLoggingEnabled` before `onCreate` completes, logging silently disables. The `get()` getter plus installing the flag as the first statement in `onCreate` mitigates this.
3. **`GOOGLE_WEB_CLIENT_ID` string interpolation.** The generated file inlines the value in a string literal. Google client IDs never contain `"` or `$`, but if the property format ever changes the generated Kotlin will not compile. Add nothing now; the build error would be immediate and obvious.
4. **Assets silently dropped from the AAR.** If `src/androidMain/assets` is not auto-wired by the new plugin, the app builds and installs but crashes at first AI/segmentation or custom-font use. V5 explicitly checks APK contents, so this cannot slip through.
5. **`implementation` dependencies of the library.** Everything moved-out stayed in, so `:androidApp`'s dependency list is a single `project(":composeApp")`. If any code is later moved to `:androidApp`, the corresponding library dependency must be re-declared there. This is the main reason the "thin shell" decision was taken.
6. **`ui-tooling` in a library.** `libs.ui.tooling` is currently an `androidMain` `implementation` dependency. On Android, `ui-tooling` is conventionally a `debugImplementation`/`androidRuntimeClasspath`-only dependency. After the split, `:androidApp` is the debug-APK producer; whether the tooling ends up in the debug APK via the library's `implementation` or must be re-declared as `androidRuntimeClasspath` in `:androidApp` is UNVERIFIED-7.
7. **Configuration cache.** `org.gradle.configuration-cache=true` is already on. The new `generateSetikerBuildConfig` task captures a plain `String` and a `Provider<Directory>`, and does not touch `project` inside `doLast`, so it should serialize. V1 runs with `--configuration-cache` to prove it and a second run must report "reused" — if it does not, switch the task body to a `Provider`-based `writeText` via `providers.fileContents` or move generation to a `@CacheableTask` class.
8. **KSP on the new plugin shape.** `add("kspAndroid", libs.room.compiler)` is assumed to keep working because the KMP Android target is still named `android`. KSP 2.3.12 already includes the `fixKspAndroidMainProcessorClasspath` fix (from KSP 2.3.6, see <https://github.com/google/ksp/issues/2476>), so the historical empty-output failure mode should not appear. V3 is the gate.
9. **AGP 9 SDK levels.** `compileSdk = 37` / `targetSdk = 37` are used today with AGP 9.4.1; the AGP 9.0 notes cite a lower max API. Since the current build compiles 37, no change is intended here — but if AGP rejects `targetSdk` in `:androidApp`, fall back to the highest level the plugin accepts and record the deviation.

### UNVERIFIED

Each item below is unconfirmed and must be closed by the listed command before the corresponding step is considered done.

| # | Unverified statement | Confirming command |
| --- | --- | --- |
| 1 | Exact post-split task names. `compileDebugKotlinAndroid`, `testDebugUnitTest`, `kspDebugKotlinAndroid`, `assembleDebug` may become single-variant names such as `compileAndroidMain` / `compileKotlinAndroid` / `testAndroidHostTest`. | `.\gradlew.bat :composeApp:tasks --all > build\tasks-after.txt` then diff against `build\tasks-before.txt` |
| 2 | `src/androidMain/assets` is auto-wired by `com.android.kotlin.multiplatform.library`. Fallback: `android { sourceSets.getByName("main").assets.srcDir("src/androidMain/assets") }`, or `androidComponents.onVariants { it.sources.assets?.addStaticSourceDirectory("src/androidMain/assets") }`. | `.\gradlew.bat :androidApp:assembleDebug` then `tar -tf .\androidApp\build\outputs\apk\debug\androidApp-debug.apk \| Select-String "assets/models/u2net.onnx"` |
| 3 | `src/androidMain/res` is auto-wired (i.e. `sourceSets["main"].res.srcDirs("src/androidMain/res")` is genuinely unnecessary). Fallback: keep the explicit `res.srcDir(...)` inside `kotlin { android { ... } }`. | `.\gradlew.bat :androidApp:processDebugResources` — unresolved `@style/Theme.Setiker` means the wiring is wrong |
| 4 | `packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }` still applies (brace expansion supported) and only needs to be declared in `:androidApp`. `packaging` also exists on the KMP library extension; whether it must be duplicated there is unknown. | `.\gradlew.bat :androidApp:assembleDebug` and inspect the APK for `META-INF/AL2.0` / `META-INF/LGPL2.1` |
| 5 | `androidKmpLibrary` (`com.android.kotlin.multiplatform.library`) at AGP 9.4.1 uses the `kotlin { android { } }` block, not the deprecated `kotlin { androidLibrary { } }`. | `.\gradlew.bat :composeApp:assemble` — an unknown-DSL error names the valid block |
| 6 | `enableKotlin = false` is valid on the `com.android.application` extension (`CommonExtension.enableKotlin`) in this AGP version. If rejected, delete the line — built-in Kotlin on a source-less module is harmless. | `.\gradlew.bat :androidApp:help` |
| 7 | `libs.ui.tooling` in the library's `androidMain` still lands in the debug APK. Alternative: declare `androidRuntimeClasspath(libs.ui.tooling)` in `:androidApp`. | `.\gradlew.bat :androidApp:assembleDebug` then check the APK's `classes*.dex` for `androidx.compose.ui.tooling` |
| 8 | The KSP configuration name stays `kspAndroid` on the new KMP Android library plugin, and only `kspAndroid` is needed (no common `ksp` config). | `.\gradlew.bat :composeApp:kspDebugKotlinAndroid` (or the name from UNVERIFIED-1) plus V3's generated-file check |
| 9 | `withHostTest {}` is required for `androidHostTest` to exist, and `androidHostTest` is the correct source-set name after the rename. | `.\gradlew.bat :composeApp:tasks --all \| Select-String "HostTest"` |
| 10 | Implicit task dependency from `kotlin.srcDir(generateSetikerBuildConfig)` (a `TaskProvider`) is honoured under configuration cache. | `.\gradlew.bat --configuration-cache :composeApp:assemble` twice; second run must report "configuration cache reused" |
| 11 | The default in AGP 9.4.1 is `android.newDsl=true` and `android.builtInKotlin=true` once those flags are removed. | Remove both lines, run `.\gradlew.bat :composeApp:help`, and confirm no deprecation warning about the legacy DSL |
| 12 | `:composeApp`'s iOS targets, `baseName = "ComposeApp"`, and `:composeApp:embedAndSignAppleFrameworkForXcode` are unaffected by the plugin swap. Cannot be verified on Windows. | On macOS: `.\gradlew.bat :composeApp:embedAndSignAppleFrameworkForXcode`, then `git diff --stat -- iosApp/` must be empty |

---

## What this does NOT change

This section is the contract of the split. If any item here changes, the change is out of scope for P3a and needs its own decision.

**Identity and packaging**

- `applicationId` stays `com.setiker.app`. It is hardcoded in `commonMain` (`BillingRepositoryImpl.kt`, `BillingApiRepository.kt`) and must not move.
- `versionCode = 1`, `versionName = "1.0"`, `minSdk = 24`, `targetSdk = 37`, `compileSdk = 37` keep their current values.
- Provider authorities keep resolving to `com.setiker.app.fileprovider` and `com.setiker.app.stickercontentprovider`.
- The `release` build still has `isMinifyEnabled = false`; no R8 keep rules are added.
- The `packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }` intent is unchanged.

**iOS / Xcode**

- `iosApp/iosApp.xcodeproj/project.pbxproj` is not edited. The build phase keeps calling `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.
- `baseName = "ComposeApp"`, `isStatic = true`, and the `iosArm64` / `iosSimulatorArm64` target set are unchanged.
- All `import ComposeApp` statements in Swift remain valid.
- Compose-resources package stays `setiker.composeapp.generated.resources`; no `Res` import changes.
- iOS is out of scope for verification on this host (Windows). Note that KSP is configured only for Android (`kspAndroid`) both before and after this change — iOS Room code generation behaviour is pre-existing and unchanged.

**Kotlin source layout**

- No `commonMain`, `androidMain`, or `iosMain` Kotlin file moves, except:
  - three one-line `BuildConfig` reference replacements (Step 7),
  - the `SetikerRuntimeFlags.install(applicationInfo)` line in `StickerApplication.onCreate`,
  - one new file `SetikerRuntimeFlags.kt`,
  - one generated file `SetikerBuildConfig.kt`.
- No package rename. No `internal` visibility widening. No DI/Koin module restructuring.
- Room `@Database(exportSchema = false)` and the absence of a schema directory stay as they are.

**Resources and assets**

- `src/androidMain/res/**` and `src/androidMain/assets/**` stay in place, byte-identical.
- `src/commonMain/composeResources/**` stays in place, byte-identical.
- Only `composeApp/src/main/res/drawable/ic_whatsapp.xml` is deleted, because it is provably dead (never wired into any source set).

**Build configuration**

- No dependency version changes. `gradle/libs.versions.toml` gains one plugin alias and loses two unused ones; no `[versions]` or `[libraries]` entry moves.
- `Gradle` wrapper, `kotlin.code.style`, JVM args, `org.gradle.workers.max`, `org.gradle.caching`, and `kotlin.native.ignoreDisabledTargets` are untouched.
- `android.nonTransitiveRClass`, `android.useAndroidX`, `android.dependency.useConstraints`, `android.r8.strictFullModeForKeepRules`, `android.generateSyncIssueWhenLibraryConstraintsAreEnabled` are untouched.
- The `GOOGLE_WEB_CLIENT_ID` source of truth remains `local.properties` (with `-PGOOGLE_WEB_CLIENT_ID` as fallback). Only the *transport* into Kotlin changes (`buildConfigField` → generated constants object).
- No CI, signing, or publishing configuration is added or modified.
