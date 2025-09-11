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
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.core)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)

            api(libs.gitlive.firebase.auth)
        }
    }
}
