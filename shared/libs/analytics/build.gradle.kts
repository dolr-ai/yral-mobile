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
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
//            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared.libs.http)
                implementation(projects.shared.core)
                implementation(projects.shared.libs.koin)
                implementation(projects.shared.libs.crashlytics)
                api(libs.gitlive.firebase.kotlin.anlaytics)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.shared.libs.preferences)
                val (deps, addRust) = BuildConfig.getAndProcessDependencies(project)
                deps.forEach { implementation(it) }
                if (addRust) implementation(projects.shared.rust)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        val iosX64Main by getting { dependsOn(iosMain) }

        val commonTest by getting {
            dependencies { implementation(libs.kotlin.test) }
        }
    }
}

android {
    namespace = "com.yral.shared.analytics"
    compileSdk = libs.versions.compileSDK.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSDK.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
