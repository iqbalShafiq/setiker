@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}


kotlin {
    // Applies to every target and to the common metadata compilation, which the Android-only
    // compilerOptions block below would not reach.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")

        // Official Kotlin Gradle Plugin option: any compiler warning fails the build.
        // The Kotlin compiler does not report unused imports (those come from the IDE), so the
        // import blocks are additionally kept clean by review; this guard covers every other
        // diagnostic, including deprecations.
        allWarningsAsErrors.set(true)
    }

    android {
        namespace = "com.setiker.app"
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Android resources are opt-in with the KMP Android library plugin.
        // src/androidMain/res must be packaged into the AAR: the app manifest references
        // @style/@mipmap/@string/@xml resources that live here.
        androidResources {
            enable = true
        }

        withHostTest {}

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
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.components.ui.tooling.preview)
            implementation(libs.ui.tooling.preview)
            implementation(libs.material.icons.extended)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.reorderable)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.ui.tooling.preview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.media3.transformer)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.common)
            implementation(libs.androidx.media3.effect)
            implementation(libs.androidx.media3.inspector.frame)
            implementation(libs.androidx.concurrent.futures.ktx)
            implementation(libs.webp.android)
            implementation(libs.coil.gif)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.onnxruntime.android)
            implementation(libs.billing.ktx)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.mockk)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)

    // Compose preview tooling is runtime-only: it must not end up on the compile classpath
    // or in the library API (AGP 9 KMP workaround).
    "androidRuntimeClasspath"(libs.ui.tooling)
}
