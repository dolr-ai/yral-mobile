import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.yral.shared.library.compose)
}

applyCocoapodsIfApple()

val firebaseIosSdkVersion =
    libs
        .versions
        .firebase
        .ios
        .sdk
        .get()

configureCocoapods {
    version = "1.0"
    summary = "Authentication feature"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    ios.deploymentTarget = "15.6"

    noPodspec()

    pod("FirebaseMessaging") {
        version = firebaseIosSdkVersion
    }
    // KMM pod() is not transitive: this module links these Firebase frameworks via
    // transitive cinterop klibs from :shared.libs.firebasePerf, :shared.libs.crashlytics,
    // :shared.libs.analytics, and :shared.libs.featureFlag (through :shared.data and
    // direct dependencies), but its own synthetic Pods project doesn't build them.
    // linkOnly adds each pod to the synthetic Podfile so the framework binary is produced
    // for linking, without generating a duplicate cinterop klib (which would cause
    // "symbol multiply defined" errors). Same pattern as iosApp/Podfile's explicit
    // Firebase pod declarations.
    pod("FirebasePerformance") {
        version = firebaseIosSdkVersion
        linkOnly = true
    }
    pod("FirebaseCrashlytics") {
        version = firebaseIosSdkVersion
        linkOnly = true
    }
    pod("FirebaseAnalytics") {
        version = firebaseIosSdkVersion
        linkOnly = true
    }
    pod("FirebaseRemoteConfig") {
        version = firebaseIosSdkVersion
        linkOnly = true
    }
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.russhwolf.multiplatformSettings.test)
            implementation(projects.shared.testSupport)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.nimbus.jose.jwt)
            implementation(libs.androidx.browser)
        }
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.data)
            implementation(projects.shared.features.wallet)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.data)
            implementation(projects.shared.libs.designsystem)
            implementation(projects.shared.libs.featureFlag)
            implementation(projects.shared.libs.phoneValidation)
        }
    }
}
