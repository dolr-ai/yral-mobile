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
        versionCode = 18
        versionName = "1.7.0"
        ndkVersion = "28.0.13004108"
        buildConfigField(
            type = "String",
            name = "BRANCH_KEY_TEST",
            value = "\"${System.getenv("YRAL_BRANCH_KEY_TEST")}\"",
        )
        buildConfigField(
            type = "String",
            name = "BRANCH_KEY",
            value = "\"${System.getenv("YRAL_BRANCH_KEY")}\"",
        )
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
            storeFile = file("my-alpha-release-key.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "android"
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
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
        }
        create("prod") {
            applicationId = "com.yral.android.app"
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.emoji2)
    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.performance)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.messaging)
    implementation(libs.lottie)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.accompanist.permission)
    implementation(libs.facebook.sdk.android.core)
    implementation(libs.mixpanel.android)
    implementation(libs.mixpanel.session.replay.android)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.decompose.decompose)
    implementation(libs.decompose.extensions.compose)

    implementation(libs.moko.permissions)
    implementation(libs.moko.permissions.compose)
    implementation(libs.moko.permissions.notifications)

    implementation(libs.branch)
    implementation(libs.play.services.ads.identifier)

    implementation(libs.googlePlay.inAppUpdate)

    implementation(projects.shared.core)
    implementation(projects.shared.data)
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
    implementation(projects.shared.libs.featureFlag)
    implementation(projects.shared.rust.service)
    implementation(projects.shared.libs.routing.deeplinkEngine)
    implementation(projects.shared.features.wallet)
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
