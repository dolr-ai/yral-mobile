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

cocoapods {
    summary = "Umbrella framework for shared KMM code"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    license = "MIT"
    ios.deploymentTarget = "15.6"
    podfile = project.file("../iosApp/Podfile")
    pod("FirebaseCore") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }
    pod("FirebaseAnalytics") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }
    pod("FirebaseCrashlytics") {
        extraOpts += listOf("-compiler-option", "-fmodules")
    }


    framework {
        baseName = "iosSharedUmbrella"
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        transitiveExport = true
        isStatic = false
//        export(projects.shared.libs.analytics)
        export(projects.shared.libs.crashlytics)
    }
}

sourceSets {
    val iosMain by creating {
        dependencies {
//            api(projects.shared.libs.analytics)
            api(projects.shared.libs.crashlytics)
        }
        }
    val iosArm64Main by getting { dependsOn(iosMain) }
    val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
    val xcf = XCFramework("iosSharedUmbrella")
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().all {
        binaries.framework {
            baseName = "iosSharedUmbrella"
            transitiveExport = true
//            export(projects.shared.libs.analytics)
            export(projects.shared.libs.crashlytics)
            xcf.add(this)
        }
    }
}