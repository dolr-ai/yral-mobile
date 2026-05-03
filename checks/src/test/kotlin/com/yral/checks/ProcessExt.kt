package com.yral.checks

import java.io.File

// When Gradle runs :checks:test the working directory is the module dir (checks/).
// The yral-mobile root is one level up.
val repoRoot: File = File(System.getProperty("user.dir")).parentFile.also { root ->
    check(root.resolve("settings.gradle.kts").exists()) {
        "Expected yral-mobile root at ${root.absolutePath} — settings.gradle.kts not found. " +
            "Check the working directory of the test task."
    }
}

fun exec(vararg cmd: String, dir: File = repoRoot): Int =
    ProcessBuilder(*cmd)
        .directory(dir)
        .inheritIO()
        .start()
        .waitFor()

fun execOrFail(vararg cmd: String, dir: File = repoRoot) {
    val exit = exec(*cmd, dir = dir)
    check(exit == 0) { "'${cmd.joinToString(" ")}' failed with exit code $exit" }
}

fun captureOutput(vararg cmd: String, dir: File = repoRoot): String =
    ProcessBuilder(*cmd)
        .directory(dir)
        .start()
        .inputStream
        .readBytes()
        .toString(Charsets.UTF_8)
