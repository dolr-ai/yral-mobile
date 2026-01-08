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
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.koin)
        }
        androidMain.dependencies {
            // Google's libphonenumber for Android
            implementation(libs.libphonenumber)
        }
        iosMain.dependencies {
            // iOS will use Foundation framework (no additional dependencies needed)
        }
    }
}
