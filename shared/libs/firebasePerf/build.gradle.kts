plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
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
        summary = "Firebase Perf"
        homepage = "https://github.com/dolr-ai/yral-mobile"
        ios.deploymentTarget = "15.6"

        noPodspec()

        pod("FirebasePerformance") {
            version = "11.14.0"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(libs.gitlive.firebase.kotlin.perf)
        }
    }
}
