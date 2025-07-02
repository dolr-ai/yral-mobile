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
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.features.auth)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.useCase)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)

            val (dependencies, shouldAddRustModule) = BuildConfig.getAndProcessDependencies(project)
            dependencies.forEach { dependency ->
                if (dependency.isNotEmpty()) {
                    implementation(dependency)
                }
            }
            if (shouldAddRustModule) {
                implementation(projects.shared.rust)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mockk)
        }
    }
}
