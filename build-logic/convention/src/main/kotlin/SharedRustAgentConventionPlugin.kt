import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class SharedRustAgentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                val configuration =
                    if (pluginManager.hasPlugin("yral.shared.library")) {
                        "commonMainImplementation"
                    } else {
                        "implementation"
                    }

                configuration(project(":shared:rust:rust-agent"))
            }
        }
    }
}
