plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinCocoapods)
}

kotlin {

    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    cocoapods {
        version = "1.0"
        summary = "Analytics module with Firebase and Mixpanel"
        homepage = "https://github.com/dolr-ai/yral-mobile"
        ios.deploymentTarget = "15.6"

        // Add Mixpanel pod
        pod("Mixpanel") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = "5.0.8"
        }
        pod("FBSDKCoreKit") {
            version = "18.0.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.libs.http)
            implementation(projects.shared.core)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.crashlytics)
            api(libs.gitlive.firebase.kotlin.anlaytics)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.rust.service)
        }
        androidMain.dependencies {
            implementation(libs.facebook.sdk.android.core)
            implementation(libs.mixpanel.android)
            implementation(libs.mixpanel.session.replay.android)
        }
    }
}
