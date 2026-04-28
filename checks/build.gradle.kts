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

    // When invoked via allChecks, run after all upstream tasks complete
    rootProject.subprojects.filter { it.path != ":checks" }.forEach { proj ->
        mustRunAfter(proj.tasks.matching { it.name in setOf("ktlintCheck", "detekt", "test", "testDebugUnitTest") })
    }
    mustRunAfter(":androidApp:assembleStagingDebug")
    rootProject.findProject(":iosSharedUmbrella")?.let {
        mustRunAfter(":iosSharedUmbrella:podInstall")
        mustRunAfter(":iosSharedUmbrella:linkPodDebugFrameworkIosSimulatorArm64")
    }
}
