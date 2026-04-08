@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.yral.buildlogic.configureIosTargets
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

fun envValue(key: String): String? {
    System.getenv(key)?.let { return it }
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("#") && "=" in trimmed) {
                val (k, v) = trimmed.split("=", limit = 2)
                if (k.trim() == key) return v.trim().removeSurrounding("\"").removeSurrounding("'")
            }
        }
    }
    return null
}

plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.gobleyCargo)
    alias(libs.plugins.gobleyUniffi)
    alias(libs.plugins.kotlinAtomicfu)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

group = "com.yral.shared"
version = "4.0.0"

kotlin {
    androidTarget {
        publishAllLibraryVariants()
    }
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies { }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/dolr-ai/yral-mobile")
            credentials {
                username = envValue("GITHUB_USERNAME")
                password = envValue("GITHUB_TOKEN")
            }
        }
    }
}

android {
    defaultConfig {
        ndkVersion = "29.0.14206865"
    }
    packaging {
        jniLibs.keepDebugSymbols += "**/*.so"
    }
}

cargo {
    // The Cargo package is located in a `rust` subdirectory.
    packageDirectory = layout.projectDirectory.dir("rust-agent-uniffi")
}

uniffi {
    // Generate the bindings using library mode.
    generateFromLibrary()
}
