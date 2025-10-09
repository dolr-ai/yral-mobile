import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class SharedRustAgentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val isDebug = isDebug(this@with)
            val isLocalRust = isLocalRust(this@with)
            dependencies {
                val dependencyNotation =
                    when {
                        isLocalRust -> project(":shared:rust:rust-agent")
                        isDebug -> "$YRAL_RUST_DEBUG:$YRAL_RUST"
                        else -> "$YRAL_RUST_RELEASE:$YRAL_RUST"
                    }

                val configuration =
                    if (this@with.plugins.hasPlugin("libs.plugins.yral.shared.library")) {
                        "androidMainImplementation"
                    } else {
                        "implementation"
                    }

                configuration(dependencyNotation)
            }
        }
    }

    private fun isDebug(project: Project): Boolean =
        project
            .findProperty("isDebug")
            ?.toString()
            ?.toBoolean() ?: true

    private fun isLocalRust(project: Project): Boolean =
        project
            .findProperty("isLocalRust")
            ?.toString()
            ?.toBoolean() ?: true

    companion object {
        private const val YRAL_RUST = "1.15.1"
        private const val YRAL_RUST_DEBUG = "com.yral.shared:rust-agent-android-debug"
        private const val YRAL_RUST_RELEASE = "com.yral.shared:rust-agent-android"
    }
}
