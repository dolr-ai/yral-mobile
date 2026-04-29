plugins { kotlin("jvm") }

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kafka.clients)
    testRuntimeOnly(libs.slf4j.simple)
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "4g"
    systemProperty("junit.jupiter.execution.timeout.default", "15m")
    systemProperty("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation")
    // Always re-run: tests hit live devices and infrastructure, so caching is meaningless.
    outputs.upToDateWhen { false }

    // The test JVM gets a minimal PATH from Gradle. Augment it with Android SDK
    // tools so that adb and emulator are available regardless of how Gradle is launched.
    val androidSdk = (System.getenv("ANDROID_HOME")?.takeIf { it.isNotBlank() }
        ?: System.getenv("ANDROID_SDK_ROOT")?.takeIf { it.isNotBlank() }
        ?: "${System.getProperty("user.home")}/Library/Android/sdk")
    val sdkAdditions = listOf("platform-tools", "emulator")
        .map { file("$androidSdk/$it") }
        .filter { it.exists() }
        .joinToString(":") { it.absolutePath }
    if (sdkAdditions.isNotEmpty()) {
        environment("PATH", "$sdkAdditions:${System.getenv("PATH") ?: ""}")
    }

    // The APK and iOS framework must exist before our JUnit tests run them.
    // Lint and unit-test ordering relative to :checks:test is handled by allChecks.dependsOn.
    mustRunAfter(":androidApp:assembleStagingDebug")
    rootProject.findProject(":iosSharedUmbrella")?.let {
        mustRunAfter(":iosSharedUmbrella:podInstall")
        mustRunAfter(":iosSharedUmbrella:linkPodDebugFrameworkIosSimulatorArm64")
    }
}
