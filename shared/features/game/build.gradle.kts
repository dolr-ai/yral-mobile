plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.yral.shared.rust.lib)
}

kotlin {
    androidTarget()
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(projects.shared.core)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.useCase)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.firebaseStore)
            implementation(projects.shared.libs.firebaseAuth)
            implementation(projects.shared.libs.http)
        }
    }
}
