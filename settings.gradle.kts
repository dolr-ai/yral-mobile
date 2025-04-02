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
include(":shared:core")
include(":shared:rust")
include(":shared:libs:preferences")
include(":shared:libs:http")
include(":shared:features:auth")