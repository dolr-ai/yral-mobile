import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Locale

plugins {
    alias(libs.plugins.yral.android.application)
    alias(libs.plugins.yral.android.application.compose)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.gms)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yral.android"
    defaultConfig {
        applicationId = "com.yral.android"
        versionCode = 1
        versionName = "1.0.0"
        ndkVersion = "28.0.13004108"
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
                nativeSymbolUploadEnabled = true
            }
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("staging") {
            dimension = "version"
            signingConfig = signingConfigs.getByName("staging")
            applicationIdSuffix = ".staging"
        }
        create("prod") {
            dimension = "version"
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.performance)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.lottie)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.accompanist.permission)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(projects.shared.core)
    implementation(projects.shared.libs.preferences)
    implementation(projects.shared.libs.http)
    implementation(projects.shared.features.auth)
    implementation(projects.shared.libs.analytics)
    implementation(projects.shared.libs.crashlytics)
    implementation(projects.shared.libs.firebaseAuth)
    implementation(projects.shared.libs.firebaseStore)
    implementation(projects.shared.libs.koin)
    implementation(projects.shared.features.feed)
    implementation(projects.shared.features.root)
    implementation(projects.shared.libs.videoPlayer)
    implementation(projects.shared.features.account)
    implementation(projects.shared.app)
    implementation(projects.shared.libs.firebasePerf)
    implementation(projects.shared.features.game)
    implementation(projects.shared.features.uploadvideo)
    implementation(projects.shared.features.profile)
    implementation(projects.shared.libs.arch)

    val (deps, addRust) = BuildConfig.getAndProcessDependencies(project)
    deps.forEach { if (it.isNotEmpty()) implementation(it) }
    if (addRust) implementation(projects.shared.rust)
}

afterEvaluate {
    android.buildTypes.forEach { buildType ->
        if (buildType.name.equals("release", ignoreCase = true)) {
            tasks
                .named(
                    "bundle${
                        buildType.name.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                    }",
                ).configure {
                    dependsOn("uploadCrashlyticsSymbolFileRelease")
                }
        }
    }
}
