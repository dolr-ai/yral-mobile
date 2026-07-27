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

// aws-lc-sys (pulled in transitively via rustls) compiles C/assembly against the
// current Xcode SDK, which defaults to the latest iOS (e.g. 26.5). The Rust
// aarch64-apple-ios target spec links at iOS 10.0 by default, so symbols like
// ___chkstk_darwin (iOS 12.0+) become undefined. Setting
// IPHONEOS_DEPLOYMENT_TARGET aligns the C build and the Rust linker at the same
// deployment target (matching the KMP/ CocoaPods deployment target of 15.6).
tasks.matching { it.name.startsWith("cargoBuild") && it.name.contains("Ios") }.configureEach {
    val cargoBuildTask = this as gobley.gradle.cargo.tasks.CargoBuildTask
    cargoBuildTask.additionalEnvironment.put("IPHONEOS_DEPLOYMENT_TARGET", "15.6")
}

tasks
    .matching {
        it.name.startsWith("runKtlintCheckOver") && it.name.endsWith("MainSourceSet")
    }.configureEach {
        dependsOn(tasks.named("buildBindings"))
    }
