import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialisartion)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

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

android {
    namespace = "com.yral.shared.features.root"
    compileSdk =
        libs.versions.compileSDK
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.minSDK
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
