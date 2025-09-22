plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.yral.shared.library.compose)
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
                implementation(compose.components.resources)
                implementation(projects.shared.libs.koin)
                implementation(projects.shared.libs.crashlytics)
                implementation(projects.shared.core)
                implementation(libs.koin.compose)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.lottie)
            }
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.resources {
    publicResClass = true
}
