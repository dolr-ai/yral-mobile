import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class SharedRustAgentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val isLocalRust = isLocalRust(this@with)
            dependencies {
                val dependencyNotation =
                    when {
                        isLocalRust -> project(":shared:rust:rust-agent")
                        else -> "$YRAL_RUST_DEPENDENCY:$YRAL_RUST_VERSION"
                    }

                val configuration =
                    if (pluginManager.hasPlugin("yral.shared.library")) {
                        "commonMainImplementation"
                    } else {
                        "implementation"
                    }

                configuration(dependencyNotation)
            }
        }
    }

    private fun isLocalRust(project: Project): Boolean =
        project
            .findProperty("isLocalRust")
            ?.toString()
            ?.toBoolean() ?: true

    companion object {
        private const val YRAL_RUST_VERSION = "2.5"
        private const val YRAL_RUST_DEPENDENCY = "com.yral.shared:rust-agent"
    }
}
