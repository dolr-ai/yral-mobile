import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureIosTargets
import com.yral.buildlogic.ifAppleBuild
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.libs.routing.routesApi)
            implementation(libs.ktor.http)
            implementation(libs.kotlinx.serialization.properties)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
