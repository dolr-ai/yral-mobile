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
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.data)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.rust.service)
            implementation(projects.shared.libs.routing.deeplinkEngine)
            implementation(projects.shared.libs.sharing)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.featureFlag)
            implementation(projects.shared.libs.designsystem)
            implementation(projects.shared.libs.videoPlayer)
            implementation(projects.shared.libs.formatters)
            implementation(projects.shared.features.reportVideo)

            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(compose.components.resources)
            implementation(libs.compose.ui.backhandler)
        }
    }
}
