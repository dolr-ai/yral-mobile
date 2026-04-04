package com.yral.android

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidVersionConfigurationTest {
    private val repoRoot = locateRepoRoot()
    private val buildFile = repoRoot.resolve("androidApp/build.gradle.kts")

    @Test
    fun `default config does not define app versions`() {
        val defaultConfigBlock = buildFile.readText().extractBlock("defaultConfig")

        assertFalse(defaultConfigBlock.contains("versionCode ="))
        assertFalse(defaultConfigBlock.contains("versionName ="))
    }

    @Test
    fun `staging and prod flavors define version metadata explicitly`() {
        val productFlavorsBlock = buildFile.readText().extractBlock("productFlavors")
        val stagingBlock = productFlavorsBlock.extractBlock("create(\"staging\")")
        val prodBlock = productFlavorsBlock.extractBlock("create(\"prod\")")

        assertTrue(stagingBlock.contains(Regex("""versionCode = \d+ // ci:staging-version-code""")))
        assertTrue(stagingBlock.contains(Regex("""versionName = "[^"]+" // ci:staging-version-name""")))
        assertTrue(prodBlock.contains(Regex("""versionCode = \d+ // ci:prod-version-code""")))
        assertTrue(prodBlock.contains(Regex("""versionName = "[^"]+" // ci:prod-version-name""")))
    }

    private fun String.extractBlock(anchor: String): String {
        val start = indexOf(anchor)
        require(start >= 0) { "Could not find block anchor '$anchor'" }

        val openBrace = indexOf('{', start)
        require(openBrace >= 0) { "Could not find opening brace for '$anchor'" }

        var depth = 0
        for (index in openBrace until length) {
            when (this[index]) {
                '{' -> {
                    depth++
                }

                '}' -> {
                    depth--
                    if (depth == 0) {
                        return substring(start, index + 1)
                    }
                }
            }
        }

        error("Could not find closing brace for '$anchor'")
    }

    private fun locateRepoRoot(): Path {
        var current = Paths.get("").toAbsolutePath().normalize()

        while (true) {
            if (
                Files.exists(current.resolve("settings.gradle.kts")) &&
                Files.exists(current.resolve("androidApp/build.gradle.kts"))
            ) {
                return current
            }

            current =
                current.parent
                    ?: error(
                        "Could not locate repository root from ${
                            Paths.get("").toAbsolutePath()
                        }",
                    )
        }
    }
}
