plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
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
            implementation(projects.shared.core)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.featureFlag)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.arch)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.paging.common)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
