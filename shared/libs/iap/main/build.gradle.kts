plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.yral.shared.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.libs.iap.core)
            implementation(projects.shared.core)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.preferences)
        }
    }
}
