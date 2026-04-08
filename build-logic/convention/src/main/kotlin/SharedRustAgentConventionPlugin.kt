import com.yral.buildlogic.isLocalRustEnabled
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class SharedRustAgentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val isLocalRust = isLocalRustEnabled()
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

    companion object {
        private const val YRAL_RUST_VERSION = "4.0.0"
        private const val YRAL_RUST_DEPENDENCY = "com.yral.shared:rust-agent"
    }
}
