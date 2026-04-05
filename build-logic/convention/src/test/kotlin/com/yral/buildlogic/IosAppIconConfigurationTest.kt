package com.yral.buildlogic

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class IosAppIconConfigurationTest {
    private val repoRoot = locateRepoRoot()

    @Test
    fun `prod and staging plists declare AppIcon`() {
        assertPlistDeclaresAppIcon(repoRoot.resolve("iosApp/Prod/Info.plist"))
        assertPlistDeclaresAppIcon(repoRoot.resolve("iosApp/Staging/Info-Staging.plist"))
    }

    @Test
    fun `app icon asset catalog contains required files`() {
        val assetCatalog = repoRoot.resolve("iosApp/iosApp/Resources/Assets.xcassets")
        val appIconSet = assetCatalog.resolve("AppIcon.appiconset")
        val requiredFiles =
            listOf(
                assetCatalog.resolve("Contents.json"),
                appIconSet.resolve("Contents.json"),
                appIconSet.resolve("Icon 1024X1024.png"),
                appIconSet.resolve("iPhone App 120X120_60pt.png"),
                appIconSet.resolve("iPhone App 180X180_60pt.png"),
            )

        requiredFiles.forEach { path ->
            assertWithMessage("Expected app icon asset file at $path")
                .that(Files.isRegularFile(path))
                .isTrue()
        }

        val contents = appIconSet.resolve("Contents.json").readText()
        assertWithMessage("App icon catalog should include the required 120x120 iPhone icon")
            .that(contents.contains("\"filename\" : \"iPhone App 120X120_60pt.png\""))
            .isTrue()
        assertWithMessage("App icon catalog should include the App Store marketing icon")
            .that(contents.contains("\"filename\" : \"Icon 1024X1024.png\""))
            .isTrue()
    }

    @Test
    fun `xcode project includes asset catalog in app resources`() {
        val project = repoRoot.resolve("iosApp/iosApp.xcodeproj/project.pbxproj").readText()

        assertWithMessage("The iOS target should still compile the AppIcon asset catalog")
            .that(project.contains("ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;"))
            .isTrue()
        assertWithMessage("The Xcode project should reference the restored asset catalog")
            .that(project.contains("058557BA273AAA24004C7B11 /* Assets.xcassets */"))
            .isTrue()
        assertWithMessage("The production iOS target should include the asset catalog in its resources build phase")
            .that(project.contains("058557BB273AAA24004C7B11 /* Assets.xcassets in Resources */"))
            .isTrue()
        assertWithMessage("The staging iOS target should include the asset catalog in its resources build phase")
            .that(project.contains("ACD941B22CFF575C0038FCB6 /* Assets.xcassets in Resources */"))
            .isTrue()
    }

    private fun assertPlistDeclaresAppIcon(path: Path) {
        val contents = path.readText()
        assertWithMessage("Expected $path to declare CFBundleIconName")
            .that(contents.contains("<key>CFBundleIconName</key>"))
            .isTrue()
        assertWithMessage("Expected $path to set CFBundleIconName to AppIcon")
            .that(contents.contains("<string>AppIcon</string>"))
            .isTrue()
    }

    private fun locateRepoRoot(): Path {
        var current = Paths.get("").toAbsolutePath().normalize()

        while (true) {
            if (
                Files.exists(current.resolve("settings.gradle.kts")) &&
                Files.exists(current.resolve("iosApp/iosApp.xcodeproj/project.pbxproj"))
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
