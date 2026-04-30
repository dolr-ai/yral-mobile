package com.yral.checks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@Tag("e2e")
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

        @AfterAll
        @JvmStatic
        fun shutdownEmulator() {
            exec("adb", "emu", "kill")
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            startEmulator()
            // Clean install: uninstall first to wipe any retained app state, then fresh install.
            exec("adb", "uninstall", appId) // ignore failure — app may not be installed yet
            execOrFail(
                "adb", "install",
                "androidApp/build/outputs/apk/staging/debug/androidApp-staging-debug.apk",
            )
            // Warm-up: launch via explicit component so the app enters the recent-tasks list.
            // Maestro's launchApp uses package-only am-start which fails on a cold install on
            // API 35+; after the app has been launched once it can be reliably re-launched.
            exec("adb", "shell", "am", "start", "-n", "$appId/.MainActivity")
            Thread.sleep(3_000)
            exec("adb", "shell", "am", "force-stop", appId)
        }

        private fun startEmulator() {
            // Wait up to 30s for any existing device (handles the case where the developer
            // already has an emulator running before invoking the e2e task).
            exec("adb", "reconnect")
            val alreadyRunning = ProcessBuilder("adb", "wait-for-device")
                .directory(repoRoot)
                .inheritIO()
                .start()
                .waitFor(30, TimeUnit.SECONDS)
            if (alreadyRunning && captureOutput("adb", "devices").lines().count { it.contains("\tdevice") } > 0) {
                println("Android device already available — skipping emulator start.")
                return
            }

            val avd = captureOutput("emulator", "-list-avds").trim().lines()
                .firstOrNull { it.isNotBlank() }
            checkNotNull(avd) { "No AVDs found — create one in Android Studio first." }

            println("Starting emulator: $avd")
            ProcessBuilder("emulator", "-avd", avd, "-no-audio", "-no-snapshot", "-no-boot-anim")
                .directory(repoRoot)
                .start() // background process; emulator outlives this @BeforeAll

            execOrFail("adb", "wait-for-device")

            val deadline = System.currentTimeMillis() + 5 * 60_000L
            while (System.currentTimeMillis() < deadline) {
                if (captureOutput("adb", "shell", "getprop", "sys.boot_completed").trim() == "1") {
                    val pmDeadline = System.currentTimeMillis() + 60_000L
                    while (System.currentTimeMillis() < pmDeadline) {
                        if (exec("adb", "shell", "pm", "list", "packages", "android") == 0) {
                            println("Emulator ready.")
                            return
                        }
                        Thread.sleep(2_000)
                    }
                    println("Emulator ready (PM check timed out, proceeding anyway).")
                    return
                }
                Thread.sleep(2_000)
            }
            error("Emulator did not finish booting within 5 minutes")
        }
    }
}
