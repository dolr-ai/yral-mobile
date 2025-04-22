import org.gradle.api.Project

object BuildConfig {
    private val yralRust = "1.1"
    private val yralRustDebug = "com.yral.shared:rust-android-debug"
    private val yralRustRelease = "com.yral.shared:rust-android"

    // This will read from gradle.properties or command line, with a default value
    fun isDebug(project: Project): Boolean = project.findProperty("isDebug")?.toString()?.toBoolean() ?: true

    // Get all dependencies based on build type
    fun getDependencies(project: Project): List<String> {
        val commonDependencies =
            listOf(
                "",
            )

        val debugOnlyDependencies =
            listOf(
                "$yralRustDebug:$yralRust",
            )

        val releaseOnlyDependencies =
            listOf(
                "$yralRustRelease:$yralRust",
            )

        return if (isDebug(project)) {
            commonDependencies + debugOnlyDependencies
        } else {
            commonDependencies + releaseOnlyDependencies
        }
    }
}
