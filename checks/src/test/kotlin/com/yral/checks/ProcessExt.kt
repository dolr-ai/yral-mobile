package com.yral.checks

import java.io.File
import java.util.ArrayDeque

// When Gradle runs :checks:test the working directory is the module dir (checks/).
// The yral-mobile root is one level up.
val repoRoot: File =
    File(System.getProperty("user.dir")).parentFile.also { root ->
        check(root.resolve("settings.gradle.kts").exists()) {
            "Expected yral-mobile root at ${root.absolutePath} — settings.gradle.kts not found. " +
                "Check the working directory of the test task."
        }
    }

fun exec(
    vararg cmd: String,
    dir: File = repoRoot,
): Int =
    ProcessBuilder(*cmd)
        .directory(dir)
        .inheritIO()
        .start()
        .waitFor()

fun execOrFail(
    vararg cmd: String,
    dir: File = repoRoot,
) {
    val exit = exec(*cmd, dir = dir)
    check(exit == 0) { "'${cmd.joinToString(" ")}' failed with exit code $exit" }
}

fun execOrFailCaptured(
    logName: String,
    vararg cmd: String,
    dir: File = repoRoot,
) {
    val logFile = repoRoot.resolve("build/check-logs/$logName.log")
    logFile.parentFile.mkdirs()

    println("Running '${cmd.joinToString(" ")}'")
    println("Full output: ${logFile.relativeTo(repoRoot)}")

    val recentLines = ArrayDeque<String>()
    val diagnosticLines = mutableListOf<String>()
    val process =
        ProcessBuilder(*cmd)
            .directory(dir)
            .redirectErrorStream(true)
            .start()

    logFile.bufferedWriter().use { writer ->
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                writer.appendLine(line)
                if (recentLines.size == RECENT_LOG_LINE_LIMIT) {
                    recentLines.removeFirst()
                }
                recentLines.addLast(line)
                if (diagnosticLines.size < DIAGNOSTIC_LOG_LINE_LIMIT && line.isDiagnosticLine()) {
                    diagnosticLines += line
                }
            }
        }
    }

    val exit = process.waitFor()
    if (exit == 0) {
        println("Command passed: '${cmd.joinToString(" ")}'")
        return
    }

    println("Command failed with exit code $exit: '${cmd.joinToString(" ")}'")
    if (diagnosticLines.isNotEmpty()) {
        println("--- diagnostic lines from ${logFile.relativeTo(repoRoot)} ---")
        diagnosticLines.forEach(::println)
    }
    println("--- recent output from ${logFile.relativeTo(repoRoot)} ---")
    recentLines.forEach(::println)
    println("--- end recent output ---")

    check(false) { "'${cmd.joinToString(" ")}' failed with exit code $exit. Full output: $logFile" }
}

fun captureOutput(
    vararg cmd: String,
    dir: File = repoRoot,
): String =
    ProcessBuilder(*cmd)
        .directory(dir)
        .start()
        .inputStream
        .readBytes()
        .toString(Charsets.UTF_8)

private fun String.isDiagnosticLine(): Boolean {
    val lower = lowercase()
    return lower.contains("error:") ||
        lower.contains("warning:") ||
        lower.contains("failed") ||
        lower.contains("failure") ||
        lower.contains("exception") ||
        lower.contains("timed out") ||
        lower.contains("timeout")
}

private const val RECENT_LOG_LINE_LIMIT = 120
private const val DIAGNOSTIC_LOG_LINE_LIMIT = 80
