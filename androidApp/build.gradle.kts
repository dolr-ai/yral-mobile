import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.*

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
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
    implementation(projects.shared.libs.analytics)
    implementation(projects.shared.libs.crashlytics)
    implementation(projects.shared.libs.koin)
    implementation(projects.shared.features.feed)
    implementation(projects.shared.features.root)

    // implementation(projects.shared.rust)
    BuildConfig.getDependencies(project).forEach { dependency ->
        if (dependency.isNotEmpty()) {
            implementation(dependency)
        }
    }

    implementation(libs.koin.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    implementation(libs.lottie)

    implementation(libs.koin.composeVM)
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
