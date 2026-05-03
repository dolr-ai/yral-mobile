plugins { kotlin("jvm") }

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kafka.clients)
    testRuntimeOnly(libs.slf4j.simple)
}

// Shared configuration applied to both test tasks.
fun org.gradle.api.tasks.testing.Test.applyCommonConfig() {
    maxHeapSize = "4g"
    systemProperty("junit.jupiter.execution.timeout.default", "30m")
    systemProperty("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation")
    // Always re-run: tests hit live devices and infrastructure, so caching is meaningless.
    outputs.upToDateWhen { false }
    // Augment PATH with Android SDK tools so adb/emulator are available regardless of launch context.
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
}

// Default test task: lint + unit tests + iOS xcodebuild checks. No Maestro e2e.
// This is what CI runs — no Android emulator or iOS device required.
tasks.test {
    useJUnitPlatform { excludeTags("e2e") }
    applyCommonConfig()
    mustRunAfter(":androidApp:assembleStagingDebug")
    rootProject.findProject(":iosSharedUmbrella")?.let {
        mustRunAfter(":iosSharedUmbrella:podInstall")
        mustRunAfter(":iosSharedUmbrella:linkPodDebugFrameworkIosSimulatorArm64")
    }
}

// e2eTest task: Maestro feed-scroll flows + Kafka assertions on real devices.
// Requires a running Android emulator and booted iOS simulator. Local-only.
tasks.register<Test>("e2eTest") {
    useJUnitPlatform { includeTags("e2e") }
    applyCommonConfig()
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    // e2e runs after the non-e2e checks have built all prerequisites (APK, iOS app).
    mustRunAfter(tasks.test)
}
