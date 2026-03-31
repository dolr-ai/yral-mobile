rootProject.name = "yral-mobile"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
    Now in Android requires JDK 17+ but it is currently using JDK ${JavaVersion.current()}.
    Java Home: [${System.getProperty("java.home")}]
    https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}

val isMacOs: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

val isLocalRust: Boolean =
    providers
        .gradleProperty("isLocalRust")
        .map { it.toBoolean() }
        .orElse(false)
        .get()

val isAppleBuild: Boolean =
    providers
        .gradleProperty("isAppleBuild")
        .map { it.toBoolean() }
        .orElse(isMacOs)
        .get()

include(":androidApp")
include(":shared:core")
include(":shared:data")
include(":shared:test-support")
if (isLocalRust) {
    include(":shared:rust:rust-agent")
}
include(":shared:rust:service")
include(":shared:libs:preferences")
include(":shared:libs:http")
include(":shared:libs:file-downloader")
include(":shared:features:auth")
include(":shared:libs:analytics")
include(":shared:libs:branch")
include(":shared:libs:crashlytics")
include(":shared:libs:koin")
include(":shared:features:feed")
include(":shared:features:root")
include(":shared:libs:videoPlayer")
include(":shared:libs:video-playback")
include(":shared:features:account")
include(":shared:libs:firebasePerf")
include(":shared:libs:firebaseAuth")
include(":shared:libs:firebaseStore")
if (isAppleBuild) {
    include(":iosSharedUmbrella")
}
include(":shared:app")
include(":shared:libs:coroutines-x")
include(":shared:libs:arch")
include(":shared:features:uploadvideo")
include(":shared:features:profile")
include(":shared:features:wallet")
include(":shared:libs:formatters")
include(":shared:libs:feature-flag")
include(":shared:libs:routing:routes-api")
include(":shared:libs:routing:deeplink-engine")
include(":shared:libs:sharing")
include(":shared:libs:designsystem")
include(":shared:libs:phone-validation")
include(":shared:libs:iap")
include(":shared:libs:iap:core")
include(":shared:libs:iap:main")
include(":shared:features:reportVideo")
include(":shared:features:chat")
include(":shared:features:subscriptions")
include(":shared:features:aiInfluencer")
