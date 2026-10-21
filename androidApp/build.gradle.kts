plugins {
    alias(libs.plugins.androidApplication)
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
configurations.configureEach {
    resolutionStrategy {
        // Compose Multiplatform 1.12.1 ships against androidx Compose 1.12.1. The UI test
        // artifacts have to agree with it exactly, or the test harness fails at runtime with a
        // NoSuchMethodError.
        force("androidx.compose.ui:ui:1.12.1")
        force("androidx.compose.ui:ui-test:1.12.1")
        force("androidx.compose.ui:ui-test-junit4:1.12.1")
        force("androidx.compose.ui:ui-test-junit4-android:1.12.1")
    }
}
