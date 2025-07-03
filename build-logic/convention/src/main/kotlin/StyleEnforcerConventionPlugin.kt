
import com.diffplug.gradle.spotless.SpotlessExtension
import com.yral.buildlogic.libs
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class StyleEnforcerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // disable style enforcement until all modules migrate to new system
        if (target.parent == null || true) return
        with(target) {
            apply(plugin = "com.diffplug.spotless")
            apply(plugin = "io.gitlab.arturbosch.detekt")

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/*/kotlin/**/*.kt")
                    targetExclude("build/**/*.kt")
                    ktfmt().kotlinlangStyle()
                }
            }
            extensions.configure<DetektExtension> {
                config.setFrom(rootProject.file("config/detekt/detekt-v2.yml"))
                buildUponDefaultConfig = true
                ignoredBuildTypes = listOf("release")
            }
            tasks.withType<Detekt>().configureEach {
                exclude {
                    it.file.absolutePath.contains("build/")
                }
            }

            // Add the Detekt dependency here
            dependencies {
                "detektPlugins"(libs.findLibrary("detekt.composeRules").get())
            }

            tasks.register("detektAll") {
                dependsOn(tasks.withType<Detekt>())
            }
        }
    }
}
