import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
}

version = "1.0"
kotlin {
    iosArm64()
    iosSimulatorArm64()
    val firebaseIos = "11.14.0"

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
        pod("FirebasePerformance") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseInstallations") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseCoreInternal") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseFirestore") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseAppCheck") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseStorage") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
        pod("FirebaseAuth") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = firebaseIos
        }
         pod("GoogleUtilities") {
            version = "8.1"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        pod("nanopb") {
            version = "3.30910.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        pod("Mixpanel") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = "5.0.8"
        }

        framework {
            baseName = "iosSharedUmbrella"
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            transitiveExport = true
            isStatic = true
            export(projects.shared.libs.analytics)
            export(projects.shared.libs.crashlytics)
            export(projects.shared.app)
        }
    }

    sourceSets {
        iosMain.dependencies {
            api(projects.shared.app)
            api(projects.shared.libs.analytics)
            api(projects.shared.libs.crashlytics)
        }
    }
}
