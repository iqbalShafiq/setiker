plugins {
    alias(libs.plugins.androidApplication)
    // The Compose compiler plugin. The app module holds no UI of its own, but both test source
    // sets do: without the plugin, `@Composable` lambdas compile to the wrong JVM signature and
    // the test harness fails at runtime with a NoSuchMethodError.
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
}

import java.util.Properties

// Dev-only secret. Source of truth: local.properties (fallback: -PGOOGLE_WEB_CLIENT_ID).
// :composeApp declares an empty default for this string resource; the app overrides it here.
val googleWebClientId: String = run {
    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { stream -> load(stream) }
    }
    localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
        ?: (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?)
        ?: ""
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

        resValue("string", "google_web_client_id", googleWebClientId)

        // The UI tests run on a device (or an emulator) against the real app.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric renders the real drawables; the default heap is too small for the
            // sticker illustrations.
            it.maxHeapSize = "2g"
        }
    }

    buildFeatures {
        // Off by default in AGP 9 (android.defaults.buildfeatures.resvalues).
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

    // Built-in Kotlin (AGP 9) is enabled so the instrumented UI tests can be written in Kotlin.
    enableKotlin = true
}

dependencies {
    implementation(project(":composeApp"))

    // --- Screenshot tests: Robolectric + Roborazzi, on the JVM, no device needed -----------------
    testImplementation(project(":composeApp"))
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.material.icons.extended)

    // --- UI and accessibility tests: on a device or an emulator -----------------------------------
    androidTestImplementation(project(":composeApp"))
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.accessibility)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.material.icons.extended)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // The Compose test harness launches a ComponentActivity; this manifest declares it.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Built-in Kotlin (AGP 9): the same warning gate the shared module uses.
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// The screenshot references live with the tests, so they are reviewed next to the code they
// belong to. `separateOutputDirs` keeps the reference, the actual image and the diff apart.
@OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/screenshots"))
    separateOutputDirs.set(true)
}

/**
 * The adb binary, taken from the SDK the build itself uses.
 */
val adbExecutable: File by lazy {
    val sdkDir = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.let { propertiesFile ->
                Properties().apply { propertiesFile.inputStream().use { load(it) } }
                    .getProperty("sdk.dir")
            }
    val binary = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
    File(sdkDir ?: "", "platform-tools/$binary")
}

fun adb(vararg arguments: String): String {
    val process = ProcessBuilder(listOf(adbExecutable.absolutePath) + arguments)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return output
}

/**
 * The connected devices, as `adb devices` reports them.
 */
fun connectedDevices(): List<String> = adb("devices")
    .lines()
    .drop(1)
    .filter { it.endsWith("device") }
    .map { it.substringBefore('\t').trim() }
    .filter { it.isNotEmpty() }

/**
 * Xiaomi's MIUI refuses to start an activity while the app sits in the background, and the
 * instrumentation restarts the app before it runs, so a Compose UI test always launches its
 * activity from the background. Without this permission the test hangs on the first assertion
 * instead of failing, which is a miserable way to spend an afternoon.
 *
 * The permission is reset whenever the package is replaced, so it has to be granted after the
 * install and right before the instrumentation starts. On every other device the command is a
 * no-op.
 */
fun grantBackgroundActivityStart(serial: String) {
    listOf("10021", "10017").forEach { operation ->
        adb("-s", serial, "shell", "appops", "set", "com.setiker.app", operation, "allow")
    }
}

/**
 * Runs the instrumented UI tests on the connected device.
 *
 * AGP's own `connectedDebugAndroidTest` installs the packages and starts the instrumentation in
 * one step, which leaves no room to grant the MIUI permission in between. This task does the
 * three steps in the order they need to happen in: install, grant, run.
 */
val connectedUiTest = tasks.register("connectedUiTest") {
    group = "verification"
    description = "Runs the instrumented UI tests on the connected device."
    dependsOn("assembleDebug", "assembleDebugAndroidTest")

    doLast {
        val devices = connectedDevices()
        if (devices.isEmpty()) {
            throw GradleException("No device is connected.")
        }

        val app = layout.buildDirectory.file("outputs/apk/debug/androidApp-debug.apk").get().asFile
        val test = layout.buildDirectory
            .file("outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk").get().asFile

        devices.forEach { serial ->
            logger.lifecycle("Installing the app on $serial")
            val appInstall = adb("-s", serial, "install", "-r", "-d", "-t", app.absolutePath)
            if (!appInstall.contains("Success")) {
                throw GradleException("The app could not be installed on $serial.\n$appInstall")
            }

            logger.lifecycle("Installing the test on $serial")
            val testInstall = adb("-s", serial, "install", "-r", "-d", "-t", test.absolutePath)
            if (!testInstall.contains("Success")) {
                throw GradleException("The test could not be installed on $serial.\n$testInstall")
            }

            // Some device makers show a dialog that counts down before the install goes through.
            // Give the package manager a moment to settle before the instrumentation starts.
            Thread.sleep(2_000)

            grantBackgroundActivityStart(serial)

            logger.lifecycle("Running the instrumented tests on $serial")
            val output = adb(
                "-s", serial, "shell", "am", "instrument", "-w", "-r",
                "com.setiker.app.test/androidx.test.runner.AndroidJUnitRunner"
            )
            logger.lifecycle(output.trim())

            val passed = output.contains("OK (") && !output.contains("FAILURES!!!")
            if (!passed) {
                throw GradleException("The instrumented tests failed on $serial.\n$output")
            }
        }
    }
}
