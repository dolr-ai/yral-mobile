import org.gradle.api.Project

object BuildConfig {
    private const val YRAL_RUST = "1.3"
    private const val YRAL_RUST_DEBUG = "com.yral.shared:rust-android-debug"
    private const val YRAL_RUST_RELEASE = "com.yral.shared:rust-android"
    private val commonDependencies = emptyList<String>()
    private val debugOnlyDependencies = listOf("$YRAL_RUST_DEBUG:$YRAL_RUST")
    private val releaseOnlyDependencies = listOf("$YRAL_RUST_RELEASE:$YRAL_RUST")

    // This will read from gradle.properties or command line, with a default value
    private fun isDebug(project: Project): Boolean =
        project
            .findProperty("isDebug")
            ?.toString()
            ?.toBoolean() ?: true

    // Get all dependencies based on build type
    private fun getDependencies(project: Project): List<String> =
        if (isDebug(project)) {
            commonDependencies + debugOnlyDependencies
        } else {
            commonDependencies + releaseOnlyDependencies
        }

    // Check if rust dependency is included in the dependencies list
    private fun hasRustDependency(dependencies: List<String>): Boolean =
        dependencies.any { dependency ->
            dependency.contains(YRAL_RUST_DEBUG) || dependency.contains(YRAL_RUST_RELEASE)
        }

    // Get all dependencies and check if rust should be added as a module
    fun getAndProcessDependencies(project: Project): Pair<List<String>, Boolean> {
        val dependencies = getDependencies(project)
        val shouldAddRustModule = !hasRustDependency(dependencies)
        return Pair(dependencies, shouldAddRustModule)
    }
}
