plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.data)
            implementation(projects.shared.features.auth)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.firebaseStore)
            implementation(projects.shared.libs.firebaseAuth)
        }
    }
}
