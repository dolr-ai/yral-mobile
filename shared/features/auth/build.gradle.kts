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
        androidMain.dependencies {
            implementation(libs.nimbus.jose.jwt)
            implementation(libs.androidx.browser)
        }
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.features.game)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.firebaseAuth)
            implementation(projects.shared.libs.firebaseStore)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.useCase)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)

            implementation(libs.gitlive.firebase.messaging)
        }
    }
}
