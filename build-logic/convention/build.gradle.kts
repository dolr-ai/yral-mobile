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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.android.lint)
}

group = "dev.yral.buildlogic"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.firebase.crashlytics.gradlePlugin)
    compileOnly(libs.firebase.performance.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.jetbrains.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    implementation(libs.truth)
    lintChecks(libs.androidx.lint.gradle)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id =
                libs.plugins.yral.android.application.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id =
                libs.plugins.yral.android.application
                    .asProvider()
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibraryCompose") {
            id =
                libs.plugins.yral.android.library.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibrary") {
            id =
                libs.plugins.yral.android.library
                    .asProvider()
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id =
                libs.plugins.yral.android.feature
                    .get()
                    .pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidTest") {
            id =
                libs.plugins.yral.android.test
                    .get()
                    .pluginId
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidFirebase") {
            id =
                libs.plugins.yral.android.application.firebase
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationFirebaseConventionPlugin"
        }
        register("androidFlavors") {
            id =
                libs.plugins.yral.android.application.flavors
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLint") {
            id =
                libs.plugins.yral.android.lint
                    .get()
                    .pluginId
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("jvmLibrary") {
            id =
                libs.plugins.yral.jvm.library
                    .get()
                    .pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("sharedLibrary") {
            id =
                libs.plugins.yral.shared.library
                    .asProvider()
                    .get()
                    .pluginId
            implementationClass = "SharedLibraryConventionPlugin"
        }
        register("sharedLibraryCompose") {
            id =
                libs.plugins.yral.shared.library.compose
                    .get()
                    .pluginId
            implementationClass = "SharedLibraryComposeConventionPlugin"
        }
        register("sharedFeature") {
            id =
                libs.plugins.yral.shared.feature
                    .get()
                    .pluginId
            implementationClass = "SharedFeatureConventionPlugin"
        }
        register("styleEnforcer") {
            id =
                libs.plugins.yral.style.enforcer
                    .get()
                    .pluginId
            implementationClass = "StyleEnforcerConventionPlugin"
        }
        register("rustAgent") {
            id =
                libs.plugins.yral.shared.rust.agent
                    .get()
                    .pluginId
            implementationClass = "SharedRustAgentConventionPlugin"
        }
    }
}
