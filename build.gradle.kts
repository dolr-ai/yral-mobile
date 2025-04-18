plugins {
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.gobleyCargo).apply(false)
    alias(libs.plugins.gobleyUniffi).apply(false)
    alias(libs.plugins.kotlinAtomicfu).apply(false)
    alias(libs.plugins.kotlinxSerialisartion).apply(false)
}

val reportMerge by tasks.registering(io.gitlab.arturbosch.detekt.report.ReportMergeTask::class) {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif")) // or "reports/detekt/merge.sarif"
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
}
