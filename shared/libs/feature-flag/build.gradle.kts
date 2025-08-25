plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.russhwolf.multiplatformSettings.core)
            implementation(libs.gitlive.firebase.config)
            implementation(libs.touchlab.logger)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
