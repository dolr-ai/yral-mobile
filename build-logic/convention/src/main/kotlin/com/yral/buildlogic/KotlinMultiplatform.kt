/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yral.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Configure base Kotlin with Android options
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = 35

        defaultConfig {
            minSdk = 24
        }

        compileOptions {
            // Up to Java 11 APIs are available through desugaring
            // https://developer.android.com/studio/write/java11-minimal-support-table
            // Update: can't use Java 11 as Gitlive SDK is compiled with Java 17
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }
    }

    configureKotlin()

    dependencies {
        "coreLibraryDesugaring"(libs.findLibrary("desugar.jdk.libs").get())
    }
}

/**
 * Configure base Kotlin options for JVM (non-Android)
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        // Up to Java 11 APIs are available through desugaring
        // https://developer.android.com/studio/write/java11-minimal-support-table
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin()
}

internal fun Project.configureKotlinMultiplatform(kotlinMultiplatformExtension: KotlinMultiplatformExtension) {
//    Disable until all modules are multiplatform

    /*kotlinMultiplatformExtension.apply {
        iosArm64()
        iosSimulatorArm64()
        jvm()
    }*/
    configureKotlin()
}

/**
 * Applies the Kotlin CocoaPods plugin only when [isAppleBuildEnabled] is true.
 *
 * Use this at the top of a module's `build.gradle.kts` instead of
 * `alias(libs.plugins.kotlinCocoapods)` inside `plugins {}` (which cannot be conditional).
 *
 * ```
 * import com.yral.buildlogic.applyCocoapodsIfApple
 * applyCocoapodsIfApple()
 * ```
 */
fun Project.applyCocoapodsIfApple() {
    if (isAppleBuildEnabled()) {
        pluginManager.apply("org.jetbrains.kotlin.native.cocoapods")
    }
}

/**
 * Configures the CocoaPods extension only when the CocoaPods plugin has been applied
 * (i.e. only on Apple builds). Uses [pluginManager.withPlugin] so the action is:
 *  - Never executed on non-Apple hosts (plugin not applied → callback never fires).
 *  - Fully type-safe via [CocoapodsExtension] in the `build-logic` classpath.
 *  - Never present as a typed lambda in the module's own `build.gradle.kts`, so the
 *    Kotlin script compiler never needs the CocoaPods DSL types to be in scope there.
 *
 * Usage in `build.gradle.kts`:
 * ```
 * import com.yral.buildlogic.applyCocoapodsIfApple
 * import com.yral.buildlogic.configureCocoapods
 *
 * applyCocoapodsIfApple()
 * configureCocoapods {
 *     version = "1.0"
 *     ios.deploymentTarget = "15.6"
 *     noPodspec()
 *     pod("SomePod") { version = "1.2.3" }
 * }
 * ```
 */
fun Project.configureCocoapods(configure: Action<CocoapodsExtension>) {
    pluginManager.withPlugin("org.jetbrains.kotlin.native.cocoapods") {
        val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.extensions.configure(CocoapodsExtension::class.java, configure)
    }
}

/**
 * Registers iosArm64 and iosSimulatorArm64 targets only when [isAppleBuildEnabled] is true.
 *
 * ```
 * import com.yral.buildlogic.configureIosTargets
 * kotlin {
 *     androidTarget()
 *     configureIosTargets(project)
 * }
 * ```
 */
fun KotlinMultiplatformExtension.configureIosTargets(project: Project) {
    if (project.isAppleBuildEnabled()) {
        iosArm64()
        iosSimulatorArm64()
    }
}

/** Executes [block] only on Apple builds. Use at the project level. */
fun Project.ifAppleBuild(block: () -> Unit) {
    if (isAppleBuildEnabled()) block()
}

/**
 * Executes [block] only on Apple builds. Use inside `kotlin { }` to guard `cocoapods { }`.
 *
 * ```
 * import com.yral.buildlogic.ifAppleBuild
 * kotlin {
 *     ifAppleBuild(project) { cocoapods { ... } }
 * }
 * ```
 */
fun KotlinMultiplatformExtension.ifAppleBuild(
    project: Project,
    block: KotlinMultiplatformExtension.() -> Unit,
) {
    if (project.isAppleBuildEnabled()) block()
}

/**
 * Configure base Kotlin options
 */
private fun Project.configureKotlin() {
    // Treat all Kotlin warnings as errors (disabled by default)
    // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
    val warningsAsErrors =
        providers
            .gradleProperty("warningsAsErrors")
            .map {
                it.toBoolean()
            }.orElse(false)

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            allWarningsAsErrors = warningsAsErrors
            freeCompilerArgs.add(
                // Enable experimental coroutines APIs, including Flow
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            )
            freeCompilerArgs.add(
                /**
                 * Remove this args after Phase 3.
                 * https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-consistent-copy-visibility/#deprecation-timeline
                 *
                 * Deprecation timeline
                 * Phase 3. (Supposedly Kotlin 2.2 or Kotlin 2.3).
                 * The default changes.
                 * Unless ExposedCopyVisibility is used, the generated 'copy' method has the same visibility as the primary constructor.
                 * The binary signature changes. The error on the declaration is no longer reported.
                 * '-Xconsistent-data-class-copy-visibility' compiler flag and ConsistentCopyVisibility annotation are now unnecessary.
                 */
                "-Xconsistent-data-class-copy-visibility",
            )
        }
    }
}
