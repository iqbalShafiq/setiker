plugins {
    alias(libs.plugins.androidApplication)
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

    // This module has no Kotlin sources of its own; built-in Kotlin is enabled by default in AGP 9.
    enableKotlin = false
}

dependencies {
    implementation(project(":composeApp"))
}
