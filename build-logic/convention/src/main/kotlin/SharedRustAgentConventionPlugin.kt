import org.gradle.api.Plugin
import org.gradle.api.Project

class SharedRustAgentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Rust agent FFI module removed — this plugin is retained as a no-op
        // placeholder until the file is deleted in a follow-up cleanup.
    }
}
