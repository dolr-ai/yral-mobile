plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

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
