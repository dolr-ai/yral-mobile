package com.yral.checks

import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File

@Order(1)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Checks {
    private val iosAppDir = File(repoRoot, "iosApp")

    @Test @Order(1)
    @EnabledOnOs(OS.MAC)
    fun `ios lint`() {
        execOrFail("swiftlint", dir = iosAppDir)
    }

    @Test @Order(2)
    @EnabledOnOs(OS.MAC)
    fun `ios setup`() {
        val localProps = File(repoRoot, "local.properties")
        if (!localProps.exists() || !localProps.readText().contains("kotlin.apple.cocoapods.bin")) {
            val podPath = captureOutput("which", "pod").trim()
            localProps.appendText("\nkotlin.apple.cocoapods.bin=$podPath\n")
        }
        execOrFail("pod", "install", dir = iosAppDir)
    }

    @Test @Order(3)
    @EnabledOnOs(OS.MAC)
    fun `ios unit tests`() {
        execOrFail(
            "xcodebuild", "test",
            "-workspace", "iosApp.xcworkspace",
            "-scheme", "iosApp",
            "-destination", "platform=iOS Simulator,name=${firstIphoneSimulatorName()}",
            "-configuration", "Debug",
            "-test-timeouts-enabled", "YES",
            "-maximum-test-execution-time-allowance", "120",
            "CODE_SIGNING_ALLOWED=NO",
            "ENABLE_USER_SCRIPT_SANDBOXING=NO",
            dir = iosAppDir,
        )
    }

    @Test @Order(4)
    @EnabledOnOs(OS.MAC)
    fun `ios build simulator app`() {
        execOrFail(
            "xcodebuild", "build",
            "-workspace", "iosApp.xcworkspace",
            "-scheme", "iosAppStaging",
            "-configuration", "Debug",
            "-destination", "platform=iOS Simulator,name=${firstIphoneSimulatorName()}",
            "-derivedDataPath", File(repoRoot, "build/DerivedData").absolutePath,
            "CODE_SIGNING_ALLOWED=NO",
            "ENABLE_USER_SCRIPT_SANDBOXING=NO",
            dir = iosAppDir,
        )
    }

    @Test @Order(5)
    fun `android start emulator`() {
        if (captureOutput("adb", "devices").lines().count { it.contains("\tdevice") } > 0) {
            println("Android device already available — skipping emulator start.")
            return
        }
        val avd = captureOutput("emulator", "-list-avds").trim().lines()
            .firstOrNull { it.isNotBlank() }
        checkNotNull(avd) { "No AVDs found — create one in Android Studio first." }

        println("Starting emulator: $avd")
        ProcessBuilder("emulator", "-avd", avd, "-no-audio", "-no-snapshot", "-no-boot-anim")
            .directory(repoRoot)
            .start() // background process; emulator outlives this test method

        execOrFail("adb", "wait-for-device")

        val deadline = System.currentTimeMillis() + 5 * 60_000L
        while (System.currentTimeMillis() < deadline) {
            if (captureOutput("adb", "shell", "getprop", "sys.boot_completed").trim() == "1") {
                // sys.boot_completed is set before the package manager is fully ready.
                // Wait until `pm list packages` succeeds to avoid install failures on cold boots.
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

    companion object {
        fun firstIphoneSimulatorName(): String =
            captureOutput("xcrun", "simctl", "list", "devices", "available")
                .lines()
                .first { it.contains("iPhone") }
                .trim()
                .replace(Regex("""\s+\(.*"""), "")

        fun firstIphoneSimulatorUdid(): String {
            val lines = captureOutput("xcrun", "simctl", "list", "devices", "available", "-j").lines()
            var pastIosRuntime = false
            val currentBlock = mutableListOf<String>()
            val udidRe = Regex(""""udid"\s*:\s*"([^"]+)"""")
            for (line in lines) {
                if (line.contains("iOS") && line.contains("com.apple.CoreSimulator.SimRuntime")) {
                    pastIosRuntime = true
                }
                if (!pastIosRuntime) continue
                when {
                    line.trim() == "{" -> currentBlock.clear()
                    line.trim().startsWith("}") -> {
                        if (currentBlock.any { it.contains("\"iPhone") }) {
                            currentBlock.firstOrNull { it.contains("\"udid\"") }
                                ?.let { udidRe.find(it)?.groupValues?.get(1) }
                                ?.let { return it }
                        }
                    }
                    else -> currentBlock.add(line)
                }
            }
            error("No iPhone simulator found")
        }
    }
}
