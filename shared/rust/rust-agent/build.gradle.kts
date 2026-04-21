@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.yral.buildlogic.configureIosTargets
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.gobleyCargo)
    alias(libs.plugins.gobleyUniffi)
    alias(libs.plugins.kotlinAtomicfu)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        publishAllLibraryVariants()
    }
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies { }
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

tasks
    .matching {
        it.name.startsWith("runKtlintCheckOver") && it.name.endsWith("MainSourceSet")
    }.configureEach {
        dependsOn(tasks.named("buildBindings"))
    }
