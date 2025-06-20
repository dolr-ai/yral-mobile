import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialisartion)
    alias(libs.plugins.kotlinCocoapods)
}
version = "1.0"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "analytics"
            isStatic = true
        }
    }

    cocoapods {
        summary = "Analytics module with Firebase and Mixpanel"
        homepage = "https://github.com/dolr-ai/yral-mobile"
        ios.deploymentTarget = "15.6"

        // Add Mixpanel pod
        pod("Mixpanel") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = "5.0.8"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.libs.http)
            implementation(projects.shared.core)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.crashlytics)
            api(libs.gitlive.firebase.kotlin.anlaytics)

        }
        androidMain.dependencies {
            implementation(projects.shared.libs.preferences)
            val (deps, addRust) = BuildConfig.getAndProcessDependencies(project)
            deps.forEach { implementation(it) }
            if (addRust) implementation(projects.shared.rust)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.yral.shared.analytics"
    compileSdk = libs.versions.compileSDK.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSDK.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
