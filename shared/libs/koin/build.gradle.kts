plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.gobleyRust)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                api(libs.koin.android)
                api(libs.koin.composeVM)
            }
        }
        commonMain {
            dependencies {
                api(libs.koin.core)
            }
        }
    }
}

android {
    namespace = "com.yral.shared.koin"
    compileSdk = libs.versions.compileSDK.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSDK.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
