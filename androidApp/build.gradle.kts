import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.*

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.play.services)
}

android {
    namespace = "com.yral.android"
    compileSdk = libs.versions.compileSDK.get().toInt()
    defaultConfig {
        applicationId = "com.yral.android"
        minSdk = libs.versions.minSDK.get().toInt()
        targetSdk = libs.versions.targetSDK.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        ndkVersion = "28.0.13004108"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("staging") {
            storeFile = file("my-debug-key.keystore")
            storePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD")
            keyAlias = "android"
            keyPassword = System.getenv("DEBUG_KEY_PASSWORD")
        }
        create("release") {
            storeFile = file("my-release-key.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "android"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("staging")
            applicationIdSuffix = ".staging"
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
                nativeSymbolUploadEnabled = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(projects.shared.core)
    implementation(projects.shared.libs.preferences)
    implementation(projects.shared.libs.http)
    implementation(projects.shared.features.auth)

    //implementation(projects.shared.rust)
    BuildConfig.getDependencies(project).forEach { dependency ->
        if (dependency.isNotEmpty()) {
            implementation(dependency)
        }
    }

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.json)
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}

afterEvaluate {
    android.buildTypes.forEach { buildType ->
        if (buildType.name.equals("release", ignoreCase = true)) {
            tasks.named(
                "bundle${
                    buildType.name.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                }"
            ).configure {
                dependsOn("uploadCrashlyticsSymbolFileRelease")
            }
        }
    }
}
