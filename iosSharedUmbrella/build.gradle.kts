import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
}

version = "1.0"
kotlin {
    iosArm64()
    iosSimulatorArm64()
    val firebaseIos = "10.25.0"

    cocoapods {
        summary = "Umbrella framework for shared KMM code"
        homepage = "https://github.com/dolr-ai/yral-mobile"
        license = "MIT"
        ios.deploymentTarget = "15.6"
        podfile = project.file("../iosApp/Podfile")
        pod("FirebaseCore") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseAnalytics") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseCrashlytics") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
    pod("FirebaseInstallations") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }
    pod("FirebaseCoreInternal") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }
    pod("GoogleUtilities") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }
    pod("nanopb") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }
    pod("Mixpanel") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }

    framework {
        baseName = "iosSharedUmbrella"
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        transitiveExport = true
        isStatic = false
        export(projects.shared.libs.analytics)
        export(projects.shared.libs.crashlytics)
    }
}

    sourceSets {
        iosMain.dependencies {
            api(projects.shared.libs.analytics)
            api(projects.shared.libs.crashlytics)
        }
    }
}