import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.*

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.gms)
    alias(libs.plugins.firebase.perf)
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
        buildConfig = true
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
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.performance)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.lottie)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

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
