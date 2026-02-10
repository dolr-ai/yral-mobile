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
                api(projects.shared.libs.crashlytics)
                api(projects.shared.libs.formatters)
                implementation(projects.shared.core)
                implementation(libs.koin.compose)
                implementation(libs.coil.compose)
                implementation(libs.coil.svg)
                implementation(libs.compottie)
                implementation(libs.compottie.dot)
                implementation(libs.compottie.network)
                implementation(libs.compottie.resources)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.window)
                implementation(libs.coil.gif)
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
