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

        val count = countSnowplowEvents(testStartMs, platformMarker = "andr-")
        assertTrue(count > 0) { "No snowplow-raw events for Android since $testStartMs" }
        println("PASS: $count event(s) for Android")
    }

    private fun runMaestroFlow() {
        exec("pkill", "-f", "maestro") // stop any lingering Maestro daemon from a previous run
        Thread.sleep(1_000)
        val serial = captureOutput("adb", "devices").lines()
            .first { it.contains("\tdevice") }.split("\t").first()
        val exit = ProcessBuilder(
            "maestro", "test",
            "--device", serial,
            "-e", "APP_ID=$appId",
            "maestro/flows/feed-scroll.yaml",
        ).directory(repoRoot).inheritIO().start().waitFor()

        if (exit != 0) {
            println("--- adb logcat (last 300 lines) ---")
            exec("adb", "logcat", "-d", "-t", "300",
                "-s", "AndroidRuntime:E", "ActivityThread:E", "ActivityManager:W", "*:F")
            println("--- end logcat ---")
            assertEquals(0, exit) { "Maestro flow failed with exit code $exit" }
        }
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
            // Warm-up: launch via explicit component so the app enters the recent-tasks list.
            // Maestro's launchApp uses package-only am-start which fails on a cold install on
            // API 35+; after the app has been launched once it can be reliably re-launched.
            exec("adb", "shell", "am", "start", "-n", "$appId/.MainActivity")
            Thread.sleep(3_000)
            exec("adb", "shell", "am", "force-stop", appId)
        }
    }
}
