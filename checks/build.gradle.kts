plugins { kotlin("jvm") }

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kafka.clients)
    testRuntimeOnly(libs.slf4j.simple)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.timeout.default", "10m")
    systemProperty("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation")
    // Gradle up-to-date check re-runs the task when E2E_PLATFORM changes
    inputs.property("e2ePlatform", System.getenv("E2E_PLATFORM") ?: "")
    inputs.property("e2eAppId", System.getenv("E2E_APP_ID") ?: "")

    // The APK and iOS framework must exist before our JUnit tests run them.
    // Lint and unit-test ordering relative to :checks:test is handled by allChecks.dependsOn.
    mustRunAfter(":androidApp:assembleStagingDebug")
    rootProject.findProject(":iosSharedUmbrella")?.let {
        mustRunAfter(":iosSharedUmbrella:podInstall")
        mustRunAfter(":iosSharedUmbrella:linkPodDebugFrameworkIosSimulatorArm64")
    }
}
