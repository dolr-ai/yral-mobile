plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
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

            val (deps, addRust) = BuildConfig.getAndProcessDependencies(project)
            deps.filter { it.isNotEmpty() }.forEach { implementation(it) }
            if (addRust) implementation(projects.shared.rust)
        }
    }
}
