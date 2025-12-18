import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Locale

plugins {
    alias(libs.plugins.yral.android.application)
    alias(libs.plugins.yral.android.application.compose)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.gms)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sentry)
}

android {
    namespace = "com.yral.android"
    defaultConfig {
        applicationId = "com.yral.android"
        versionCode = 50
        versionName = "2.4.0"
        ndkVersion = "28.0.13004108"
        buildConfigField(
            type = "String",
            name = "BRANCH_KEY_TEST",
            value = "\"key_test_cuuos6m3G17sHqacdUgoYbpbFBkSpEFs\"",
        )
        buildConfigField(
            type = "String",
            name = "BRANCH_KEY",
            value = "\"${System.getenv("YRAL_BRANCH_KEY")}\"",
        )
        buildConfigField(
            type = "String",
            name = "META_INSTALL_REFERRER_DECRYPTION_KEY",
            value = "\"${System.getenv("YRAL_META_INSTALL_REFERRER_DECRYPTION_KEY") ?: ""}\"",
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes +=
                setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                )
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

sentry {
    // Prevent Sentry dependencies from being included in the Android app through the AGP.
    autoInstallation {
        enabled.set(false)
    }

    // The slug of the Sentry organization to use for uploading proguard mappings/source contexts.
    org.set(System.getenv("SENTRY_ORG"))
    // The slug of the Sentry project to use for uploading proguard mappings/source contexts.
    projectName.set(System.getenv("SENTRY_PROJECT"))
    // The authentication token to use for uploading proguard mappings/source contexts.
    // WARNING: Do not expose this token in your build.gradle files, but rather set an environment
    // variable and read it into this property.
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN"))
    url.set(System.getenv("SENTRY_URL"))

    ignoredBuildTypes.set(setOf("debug"))
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.emoji2)
    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry.compose.android)
    implementation(libs.sentry.android.navigation)
    implementation(libs.sentry.android.fragment)
    implementation(libs.sentry.android.sqlite)
    implementation(libs.sentry.okhttp)
    implementation(libs.sentry.kotlin.extensions)
    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.performance)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.messaging)
    implementation(libs.coil.compose)
    implementation(libs.coil.ktor3)
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
    implementation(libs.installreferrer)

    implementation(libs.googlePlay.inAppUpdate)

    implementation(libs.googlePlay.billingclient)
    implementation(libs.googlePlay.billingclient.ktx)

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
    implementation(projects.shared.libs.designsystem)
    implementation(projects.shared.features.reportVideo)
    implementation(compose.components.resources)
    implementation(projects.shared.features.leaderboard)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
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
