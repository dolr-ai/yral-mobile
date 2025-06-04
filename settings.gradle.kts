enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "yral-mobile"
include(":composeApp")
include(":androidApp")
include(":shared:core")
include(":shared:rust")
include(":shared:libs:preferences")
include(":shared:libs:http")
include(":shared:libs:firebasePerf")
include(":shared:features:auth")
include(":shared:libs:analytics")
include(":shared:libs:crashlytics")
include(":shared:libs:koin")
include(":shared:features:feed")
include(":shared:features:root")
include(":shared:libs:videoPlayer")
include(":shared:features:account")
include(":shared:libs:useCase")
