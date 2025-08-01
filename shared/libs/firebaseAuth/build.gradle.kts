plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
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
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.core)
            implementation(projects.shared.libs.useCase)
            implementation(projects.shared.libs.crashlytics)

            api(libs.gitlive.firebase.auth)
        }
    }
}
