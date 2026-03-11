import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureIosTargets
import com.yral.buildlogic.ifAppleBuild
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

kotlin {
    androidTarget()
    configureIosTargets(project)
    sourceSets {
        commonMain.dependencies {
            // No external dependencies; vendored implementations
        }
    }
}
