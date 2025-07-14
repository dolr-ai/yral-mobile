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
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.features.auth)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.useCase)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.firebaseStore)

            val (deps, addRust) = BuildConfig.getAndProcessDependencies(project)
            deps.forEach { if (it.isNotEmpty()) implementation(it) }
            if (addRust) implementation(projects.shared.rust)
        }
    }
}
