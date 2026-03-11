/*
 * Copyright 2023 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.yral.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Returns true when Apple toolchains (iOS/macOS targets) should be compiled.
 *
 * Resolution order:
 *  1. If `isAppleBuild` is explicitly set in gradle.properties or on the CLI, that value wins.
 *     This lets macOS CI jobs opt-out and Windows developers opt-in (cross-compile) if needed.
 *  2. Otherwise, the host OS is queried automatically: macOS → true, everything else → false.
 *
 * In practice, no one needs to touch gradle.properties – it just works on every OS.
 */
fun Project.isAppleBuildEnabled(): Boolean {
    val isMacOs = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)
    return providers
        .gradleProperty("isAppleBuild")
        .map { it.toBoolean() }
        .orElse(isMacOs)
        .get()
}

/**
 * Returns true when the local Rust source (`:shared:rust:rust-agent`) should be used
 * instead of the pre-built Maven artifact.
 *
 * The gobley-cargo plugin only compiles Cargo for the Kotlin targets that are registered in the
 * `kotlin {}` block. Because [isAppleBuildEnabled] already gates iOS targets, on a non-Apple build
 * gobley-cargo will only see `androidTarget()` and will only invoke the Android NDK toolchain —
 * no macOS or Apple toolchain is required.
 *
 * Therefore local Rust is allowed on **any** OS as long as:
 *  - The Rust toolchain + Cargo are installed on the host, AND
 *  - The Android NDK is available (set via `ndkVersion` in the module's `android {}` block).
 *
 * The flag is opt-in: set `isLocalRust=true` in gradle.properties (or pass `-PisLocalRust=true`
 * on the CLI). It defaults to false so that developers who have not set up the Rust/NDK toolchain
 * transparently fall back to the pre-built Maven artifact.
 *
 * On macOS with iOS targets enabled, the full Cargo cross-compilation (Android + iOS) runs.
 * On Windows/Linux with iOS targets disabled, only the Android NDK Cargo build runs.
 */
fun Project.isLocalRustEnabled(): Boolean =
    providers
        .gradleProperty("isLocalRust")
        .map { it.toBoolean() }
        .orElse(false)
        .get()

