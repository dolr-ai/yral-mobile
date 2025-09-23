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
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinResult.core)
            api(libs.kotlinResult.coroutines)
            implementation(libs.ktor.serialization.kotlinx.json)

            api(libs.touchlab.logger)

            implementation(projects.shared.libs.koin)
            implementation(projects.shared.core)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.rust.service)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
