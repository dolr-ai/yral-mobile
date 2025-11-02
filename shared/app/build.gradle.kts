plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.yral.shared.library.compose)
}

version = "1.0"
kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.features.reportVideo)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.featureFlag)
            implementation(projects.shared.libs.sharing)
            implementation(projects.shared.libs.designsystem)
            implementation(projects.shared.libs.videoPlayer)
            implementation(projects.shared.data)

            // Routing framework dependencies
            implementation(projects.shared.libs.routing.routesApi)
            implementation(projects.shared.libs.routing.deeplinkEngine)

            implementation(projects.shared.libs.firebaseStore)
            implementation(projects.shared.libs.firebaseAuth)
            implementation(projects.shared.libs.firebasePerf)
            implementation(libs.gitlive.firebase.storage)
            implementation(projects.shared.features.auth)
            implementation(projects.shared.features.feed)
            implementation(projects.shared.features.root)
            implementation(projects.shared.features.account)
            implementation(projects.shared.features.game)
            implementation(projects.shared.features.uploadvideo)
            implementation(projects.shared.features.profile)
            implementation(projects.shared.features.wallet)
            implementation(projects.shared.features.leaderboard)
            implementation(projects.shared.rust.service)
            implementation(libs.coil.compose)
            implementation(libs.coil.ktor3)

            implementation(compose.components.resources)

            implementation(libs.decompose.decompose)
            implementation(libs.decompose.extensions.compose)

            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.compose)
            implementation(libs.moko.permissions.notifications)

            implementation(libs.androidx.paging.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
