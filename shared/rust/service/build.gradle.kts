@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.yral.shared.rust.agent)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.core)
            implementation(projects.shared.data)

            api(libs.kotlinResult.core)
            api(libs.kotlinResult.coroutines)

            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
