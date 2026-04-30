package com.yral.checks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File

@Tag("e2e")
@Order(3)
@EnabledOnOs(OS.MAC)
class IosE2eTest {
    @BeforeEach
    fun requireLiveDevice() {
        val booted = runCatching {
            captureOutput("xcrun", "simctl", "list", "devices").contains("(Booted)")
        }.getOrDefault(false)
        assertTrue(booted, "iOS E2E requires a booted simulator")
    }

    @Test
    fun `feed scroll delivers events to snowplow-raw`() {
        val testStartMs = System.currentTimeMillis()

        runMaestroFlow()

        println("Waiting 15s for collector → snowplow-raw delivery...")
        Thread.sleep(15_000)

        val count = countSnowplowEvents(testStartMs, platformMarker = "ios-")
        assertTrue(count > 0) { "No snowplow-raw events for iOS since $testStartMs" }

        val videoCount = countSnowplowEvents(testStartMs, platformMarker = "ios-", eventMarker = "video_viewed", minCount = 2)
        assertTrue(videoCount >= 2) { "Expected ≥2 video_viewed events for iOS, got $videoCount" }
        println("PASS: $count total event(s), $videoCount video_viewed for iOS")
    }

    private fun runMaestroFlow() {
        exec("pkill", "-f", "maestro") // stop any lingering Maestro daemon from a previous run
        Thread.sleep(1_000)
        val exit = ProcessBuilder(
            "maestro", "test",
            "--device", Checks.firstIphoneSimulatorUdid(),
            "-e", "APP_ID=$appId",
            "maestro/flows/feed-scroll.yaml",
        ).directory(repoRoot).inheritIO().start().waitFor()
        assertEquals(0, exit) { "Maestro flow failed with exit code $exit" }
    }

    companion object {
        private const val appId = "com.yral.iosApp.staging"

        @AfterAll
        @JvmStatic
        fun shutdownSimulator() {
            exec("xcrun", "simctl", "shutdown", "booted")
        }

        @BeforeAll
        @JvmStatic
        fun installApp() {
            val udid = Checks.firstIphoneSimulatorUdid()
            exec("xcrun", "simctl", "boot", udid) // ignore error if already booted
            execOrFail("xcrun", "simctl", "bootstatus", udid, "-b")
            exec("open", "-a", "Simulator") // bring the Simulator window up so it's visible locally
            // Clean install: uninstall first to wipe retained app state, then fresh install.
            exec("xcrun", "simctl", "uninstall", "booted", appId) // ignore failure if not installed
            execOrFail(
                "xcrun", "simctl", "install", "booted",
                File(repoRoot, "build/DerivedData/Build/Products/Debug-iphonesimulator/Yral-Staging.app").absolutePath,
            )
        }
    }
}
