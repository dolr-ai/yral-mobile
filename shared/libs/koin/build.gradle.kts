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
        androidMain {
            dependencies {
                api(libs.koin.android)
                api(libs.koin.compose.viewmodel)
            }
        }
        commonMain {
            dependencies {
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
            }
        }
    }
}
