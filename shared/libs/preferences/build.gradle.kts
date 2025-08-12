plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gobleyRust)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        androidMain.dependencies {
            api(libs.androidx.security.crypto)
        }
        commonMain.dependencies {
            api(libs.russhwolf.multiplatformSettings.core)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(projects.shared.core)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.koin)
        }
    }
}
