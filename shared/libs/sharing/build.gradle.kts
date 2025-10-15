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
        summary = "Sharing module with Branch"
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
                implementation(projects.shared.libs.coroutinesX)
            }
        }
        androidMain.dependencies {
            implementation(libs.branch)
            implementation(libs.play.services.ads.identifier)
            api(libs.coil.core)
        }
    }
}
