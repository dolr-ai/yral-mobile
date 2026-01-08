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
        summary = "Branch SDK module providing Branch dependencies and cinterop"
        homepage = "https://github.com/dolr-ai/yral-mobile"
        ios.deploymentTarget = "15.6"

        noPodspec()

        pod("BranchSDK") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = "3.12.1"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // No common dependencies needed
            }
        }
        androidMain.dependencies {
            // Expose as API so consuming modules can use these dependencies
            api(libs.branch)
            api(libs.play.services.ads.identifier)
        }
    }
}
