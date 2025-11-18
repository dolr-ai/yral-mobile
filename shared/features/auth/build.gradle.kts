plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.yral.shared.library.compose)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

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
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.rust.service)
            implementation(projects.shared.libs.designsystem)
            implementation(projects.shared.libs.featureFlag)

            implementation(libs.gitlive.firebase.messaging)

            implementation(compose.components.resources)
        }
    }
}
