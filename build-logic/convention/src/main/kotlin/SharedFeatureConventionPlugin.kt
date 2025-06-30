import com.yral.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class SharedFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "yral.shared.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "commonMainImplementation"(libs.findLibrary("kotlinx.coroutines.core").get())
                "commonMainImplementation"(libs.findLibrary("kotlinx.serialization.json").get())
                "commonMainImplementation"(libs.findLibrary("decompose.decompose").get())
                "commonMainImplementation"(libs.findLibrary("decompose.extensions.compose").get())
                "commonMainImplementation"(libs.findLibrary("essenty.lifecycle.coroutines").get())

                val bom = libs.findLibrary("koin.bom").get()
                "commonMainImplementation"(platform(bom))
                "commonMainImplementation"(libs.findLibrary("koin.core").get())
            }
        }
    }
}
