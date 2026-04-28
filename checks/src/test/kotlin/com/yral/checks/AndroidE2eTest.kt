package com.yral.checks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@Order(2)
class AndroidE2eTest {
    @BeforeEach
    fun requireLiveDevice() {
        val connected = runCatching {
            captureOutput("adb", "devices").lines().count { it.contains("\tdevice") } > 0
        }.getOrDefault(false)
        assertTrue(connected, "Android E2E requires a connected emulator/device")
    }

    @Test
    fun `feed scroll delivers events to snowplow-raw`() {
        val testStartMs = System.currentTimeMillis()

        runMaestroFlow()

        println("Waiting 15s for collector → snowplow-raw delivery...")
        Thread.sleep(15_000)

        val count = countSnowplowEvents(testStartMs, platformMarker = "Android")
        assertTrue(count > 0) { "No snowplow-raw events for Android since $testStartMs" }
        println("PASS: $count event(s) for Android")
    }

    private fun runMaestroFlow() {
        val exit = ProcessBuilder(
            "maestro", "test",
            "-e", "APP_ID=$appId",
            "maestro/flows/feed-scroll.yaml",
        ).directory(repoRoot).inheritIO().start().waitFor()
        assertEquals(0, exit) { "Maestro flow failed with exit code $exit" }
    }

    companion object {
        private const val appId = "com.yral.android"

        @BeforeAll
        @JvmStatic
        fun installApk() {
            execOrFail(
                "adb", "install", "-r",
                "androidApp/build/outputs/apk/staging/debug/androidApp-staging-debug.apk",
            )
        }
    }
}
