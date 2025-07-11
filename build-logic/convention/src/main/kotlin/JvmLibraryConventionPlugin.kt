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

import com.yral.buildlogic.configureKotlinJvm
import com.yral.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val isKmp = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")
            if (!isKmp) {
                apply(plugin = "org.jetbrains.kotlin.jvm")
            }

//            Temporarily disable lint from jvm as it conflicts with android lint
//            apply(plugin = "yral.android.lint")
            apply(plugin = "yral.style.enforcer")

            configureKotlinJvm()

            dependencies {
                if (!isKmp) {
                    "testImplementation"(libs.findLibrary("kotlin.test").get())
                }
            }
        }
    }
}
