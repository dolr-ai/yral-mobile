plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.gobleyCargo).apply(false)
    alias(libs.plugins.gobleyUniffi).apply(false)
    alias(libs.plugins.kotlinAtomicfu).apply(false)
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

val reportMerge by tasks.registering(io.gitlab.arturbosch.detekt.report.ReportMergeTask::class) {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif")) // or "reports/detekt/merge.sarif"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/dolr-ai/yral-mobile")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    ktlint {
        val ktlintFiles = project.findProperty("ktlintFiles") as? String
        if (ktlintFiles != null) {
            filter {
                include(ktlintFiles.split("\n"))
            }
        }
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        }
    }

    detekt {
        toolVersion = "1.23.8"
        config.from(files("$rootDir/detekt-config.yml"))
        buildUponDefaultConfig = true

        val detektFiles = project.findProperty("detektFiles") as? String
        if (detektFiles != null) {
            source.from(files(detektFiles.split(",")))
        } else {
            source.setFrom(
                "$projectDir/src/commonMain/kotlin",
                "$projectDir/src/androidMain/kotlin",
                "$projectDir/src/iosMain/kotlin",
                "$projectDir/src/main/kotlin",
            )
        }
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        exclude { fileTreeElement ->
            val path = fileTreeElement.file.absolutePath
            path.contains("/build/") ||
                path.endsWith("build.gradle.kts") ||
                path.endsWith("settings.gradle.kts")
        }
        basePath = rootProject.projectDir.absolutePath
        reports {
            xml {
                required.set(true)
                outputLocation.set(file("build/reports/detekt/detekt.xml"))
            }
            sarif {
                required.set(true)
                outputLocation.set(file("build/reports/detekt/detekt.sarif"))
            }
        }
        finalizedBy(reportMerge)
        reportMerge.configure {
            input.from(sarifReportFile)
        }
    }

    // Black listing alpha version of compose ui since 1.10.3-alphaX dependency
    // required by androidx-paging 3.4.0-alphaX is buggy
    configurations.configureEach {
        dependencies.withType<ExternalModuleDependency>().configureEach {
            if (group == "androidx.paging" && version?.contains("alpha") == true) {
                exclude(group = "androidx.compose.ui", module = "ui")
            }
        }
    }
}

// Validate IAP core module usage - only iap:main should depend on iap:core
gradle.projectsEvaluated {
    val coreModulePath = ":shared:libs:iap:core"
    val allowedDependentPath = ":shared:libs:iap:main"
    val violations = mutableListOf<String>()

    allprojects.forEach { project ->
        if (project.path == coreModulePath) return@forEach

        project.configurations.forEach { config ->
            if (!config.isCanBeResolved) return@forEach

            config.dependencies.withType<ProjectDependency>().forEach { dependency ->
                try {
                    // Use dependencyProject to get the target project (even if deprecated, it works)
                    @Suppress("DEPRECATION")
                    val dependencyProject = dependency.dependencyProject
                    val dependencyPath = dependencyProject.path

                    if (dependencyPath == coreModulePath && project.path != allowedDependentPath) {
                        violations.add(
                            "❌ ${project.path} depends on $coreModulePath via ${config.name}. " +
                                "Use $allowedDependentPath instead.",
                        )
                    }
                } catch (_: Exception) {
                    // Skip if can't resolve
                }
            }
        }
    }

    if (violations.isNotEmpty()) {
        val violationsList = violations.joinToString("\n") { "  • $it" }
        val errorMessage =
            """
            |Module Usage Violation: Only $allowedDependentPath should depend on $coreModulePath. 
            |Violations found:
            |$violationsList
            |Fix: Replace dependency on '$coreModulePath' with '$allowedDependentPath'
            """.trimMargin()

        throw GradleException(errorMessage)
    }
}
