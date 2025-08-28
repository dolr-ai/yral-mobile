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

include(":androidApp")
include(":shared:core")
include(":shared:data")
include(":shared:rust:rust-agent")
include(":shared:rust:service")
include(":shared:libs:preferences")
include(":shared:libs:http")
include(":shared:features:auth")
include(":shared:libs:analytics")
include(":shared:libs:crashlytics")
include(":shared:libs:koin")
include(":shared:features:feed")
include(":shared:features:root")
include(":shared:libs:videoPlayer")
include(":shared:features:account")
include(":shared:libs:useCase")
include(":shared:libs:firebasePerf")
include(":shared:features:game")
include(":shared:libs:firebaseAuth")
include(":shared:libs:firebaseStore")
include(":iosSharedUmbrella")
include(":shared:app")
include(":shared:libs:coroutines-x")
include(":shared:libs:arch")
include(":shared:features:uploadvideo")
include(":shared:features:profile")
include(":shared:libs:feature-flag")
